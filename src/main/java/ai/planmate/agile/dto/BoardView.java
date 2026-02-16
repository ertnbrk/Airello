package ai.planmate.agile.dto;

import java.util.List;
import java.util.Map;

import ai.planmate.agile.entity.Issue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoardView {
    private Map<String, List<Issue>> columns;
}
