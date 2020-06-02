package io.snowdrop.reporting.weekreport;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import io.snowdrop.reporting.MarkdownHelper;
import io.snowdrop.reporting.ReportConstants;
import io.snowdrop.reporting.model.Issue;
import net.steppschuh.markdowngenerator.MarkdownSerializationException;
import net.steppschuh.markdowngenerator.link.Link;
import net.steppschuh.markdowngenerator.list.UnorderedList;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.TextBuilder;

/**
 * <p>Generates the Weekly Development report.</p>
 * <p>This report consists of all the Issue existing in the weekly development repository that have the following characteristics:
 * <ul>
 *     <li>Have been modified in the specified date range or are still open</li>
 *     <li>Are not labeled report or have no label at all</li>
 * </ul>
 * </p>
 * <p>The SQL sentence used is the following: <pre>repository = ?1 AND ((updatedAt >= ?2 AND updatedAt <= ?3) OR open = true) AND (label != 'report' or label is null)</pre></p>
 * <p>For the presentation, the issues are grouped by assignee and label.</p>
 */
public class WeeklyDevelopmentReportImpl {

  /**
   * <p>Users to which the report will be applied</p>
   */
  protected Set<String> users;

  /**
   * <p>Identifies the begining of the time period for the report</p>
   */
  protected Date startDate = null;

  /**
   * <p>Identifies the begining of the time period for the report</p>
   */
  protected Date endDate = null;

  public WeeklyDevelopmentReportImpl(final Date pStartDate, final Date pEndDate, final Set<String> pusers) {
    users = pusers;
    startDate = pStartDate;
    endDate = pEndDate;
  }

  public static WeeklyDevelopmentReportImpl build(final Date pStartDate, final Date pEndDate, final Set<String> pusers) {
    return new WeeklyDevelopmentReportImpl(pStartDate, pEndDate, pusers);
  }

  /**
   * <p>Generates the markdown for the report.</p>
   *
   * @return
   */
  public String buildWeeklyReport() {
    StringBuilder sb = new StringBuilder();
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime twoWeeksAgo = now.minusWeeks(2);
    ZonedDateTime oneMonthAgo = now.minusMonths(1);
    String repoName = ReportConstants.WEEK_DEV_REPO_OWNER + "/" + ReportConstants.WEEK_DEV_REPO_NAME;
    Issue.findByIssuesForWeeklyDevelopmentReport(repoName, startDate, endDate).list().stream()
        .collect(Collectors.groupingBy(Issue::getAssignee, Collectors.toSet()))
        .entrySet().forEach(eachAssignee -> {
      String assignee = eachAssignee.getKey();
      sb.append(ReportConstants.CR).append(ReportConstants.CR).append(MarkdownHelper.addHeadingTitle(assignee, 2)).append(ReportConstants.CR);
      UnorderedList labelUnorderedList = new UnorderedList();
      eachAssignee.getValue().stream().collect(Collectors.groupingBy(Issue::getLabel, Collectors.toSet())).entrySet().forEach(eachLabel -> {
        String label = eachLabel.getKey();
        labelUnorderedList.getItems().add(label);
        UnorderedList issueUnorderedList = new UnorderedList();
        eachLabel.getValue().stream().forEach(eachIssue -> {
          TextBuilder issueTextB = new TextBuilder();
          Date dateCreatedAt = eachIssue.getCreatedAt();
          String strMdColor = "gray";
          if(eachIssue.isOpen()) {
            if (dateCreatedAt.toInstant().isBefore(oneMonthAgo.toInstant())) {
              strMdColor = "red";
            } else if (dateCreatedAt.toInstant().isBefore(twoWeeksAgo.toInstant())) {
              strMdColor = "orange";
            } else {
              strMdColor = "green";
            }
          }
          issueTextB.append(new Text("<span style=\"color:" + strMdColor + "\">[")).append(eachIssue.getStatus())
              .append("]</span> ").append(eachIssue.getTitle()).append(" - ").append(new Link(eachIssue.getUrl()));
          issueUnorderedList.getItems().add(issueTextB);
        });
        labelUnorderedList.getItems().add(issueUnorderedList);
      });
      try {
        sb.append(labelUnorderedList.serialize());
      } catch (MarkdownSerializationException pE) {
        pE.printStackTrace();
      }
    });
    return sb.toString();
  }

}