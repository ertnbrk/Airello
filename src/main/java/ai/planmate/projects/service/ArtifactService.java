package ai.planmate.projects.service;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.dto.ArtifactUploadResponse;
import ai.planmate.projects.entity.Artifact;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.repository.ArtifactRepository;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@ConditionalOnProperty(name = "planmate.features.artifacts-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ArtifactService {

    private static final List<String> ALLOWED_CONTENT_TYPES =
            Arrays.asList(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain",
                    "text/markdown");

    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB

    private final ArtifactRepository artifactRepository;
    private final ProjectRepository projectRepository;
    private final AppUserRepository appUserRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public ArtifactUploadResponse uploadArtifact(UUID projectId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds 50 MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Invalid file type. Allowed types: PDF, Word, Text, Markdown");
        }

        // Without authentication, uploadedBy will be null. Consider adding userId parameter if
        // needed.
        AppUser currentUser = null;

        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        String s3Key = generateS3Key(projectId, file.getOriginalFilename());

        try {
            uploadToS3(s3Key, file);
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file");
        }

        Artifact artifact = new Artifact();
        artifact.setProject(project);
        artifact.setName(file.getOriginalFilename());
        artifact.setContentType(contentType);
        artifact.setSizeBytes(file.getSize());
        artifact.setS3Bucket(bucketName);
        artifact.setS3Key(s3Key);
        artifact.setUploadedBy(currentUser);
        artifact.setUploadCompleted(true);

        artifact = artifactRepository.save(artifact);

        String downloadUrl = generatePresignedUrl(s3Key);

        return new ArtifactUploadResponse(
                artifact.getId(),
                artifact.getName(),
                artifact.getSizeBytes(),
                artifact.getContentType(),
                downloadUrl);
    }

    private String generateS3Key(UUID projectId, String filename) {
        return String.format("projects/%s/artifacts/%s-%s", projectId, UUID.randomUUID(), filename);
    }

    private void uploadToS3(String s3Key, MultipartFile file) throws IOException {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
    }

    private String generatePresignedUrl(String s3Key) {
        GetObjectPresignRequest getObjectRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(req -> req.bucket(bucketName).key(s3Key).build())
                        .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(getObjectRequest);
        return presignedRequest.url().toString();
    }
}
