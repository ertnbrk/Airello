# Contributing to Airello

Thank you for your interest in contributing to Airello! This document provides guidelines for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/airello.git`
3. Create a feature branch: `git checkout -b feature/your-feature-name`
4. Make your changes
5. Commit your changes: `git commit -m 'Add some feature'`
6. Push to the branch: `git push origin feature/your-feature-name`
7. Open a Pull Request

## Development Setup

### Prerequisites

- Java 21
- Docker & Docker Compose
- Python 3.11+ (for AI worker)

### Local Development

```bash
# Start infrastructure
docker compose up -d postgres redis

# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

## Code Style

We use Google Java Style (AOSP variant) with automated formatting:

```bash
# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Run linter
./gradlew checkstyleMain
```

## Commit Messages

- Use clear, descriptive commit messages
- Follow conventional commits format: `type(scope): description`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
```
feat(board): add drag and drop support
fix(auth): resolve JWT expiration issue
docs(readme): update installation instructions
```

## Pull Request Guidelines

1. **Keep PRs focused** - One feature/fix per PR
2. **Write tests** - Add unit tests for new features
3. **Update documentation** - Update README.md if needed
4. **Follow code style** - Run `./gradlew spotlessApply` before committing
5. **Pass all checks** - Ensure `./gradlew check` passes
6. **Write clear descriptions** - Explain what and why, not just how

## Testing

- Write unit tests for business logic
- Add integration tests for API endpoints
- Ensure all tests pass before submitting PR
- Aim for >80% code coverage

## Reporting Issues

When reporting issues, please include:

1. **Clear title and description**
2. **Steps to reproduce**
3. **Expected behavior**
4. **Actual behavior**
5. **Environment details** (OS, Java version, Docker version)
6. **Logs or error messages**

## Feature Requests

We welcome feature requests! Please:

1. Check existing issues first
2. Describe the problem you're trying to solve
3. Propose a solution if you have one
4. Explain why this feature would be useful

## Code Review Process

1. Maintainers will review PRs within 7 days
2. Address review feedback promptly
3. Once approved, maintainers will merge
4. PRs may be closed after 30 days of inactivity

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

## Questions?

Feel free to open an issue for questions or join our discussions!
