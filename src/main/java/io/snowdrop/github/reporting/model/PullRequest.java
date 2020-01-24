package io.snowdrop.github.reporting.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class PullRequest extends PanacheEntityBase implements WithDates {

  @Id
  String url;
  String repository;
  int number;
  String title;
  String creator;
  String assignee;
  boolean open;
  Date createdAt;
  Date updatedAt;
  Date closedAt;

  public PullRequest() {

  }

  public PullRequest(String url, String repository, int number, String title, String creator, String assignee,
      boolean open, Date createdAt, Date updatedAt, Date closedAt) {
    this.url = url;
    this.repository = repository;
    this.number = number;
    this.title = title;
    this.creator = creator;
    this.assignee = assignee;
    this.open = open;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.closedAt = closedAt;
  }

  public static PullRequest create(String repository, org.eclipse.egit.github.core.PullRequest pr) {
    return new PullRequest(pr.getHtmlUrl(), repository, pr.getNumber(), pr.getTitle(), pr.getUser().getLogin(), null,
        pr.getState().equals("open"), pr.getCreatedAt(), pr.getUpdatedAt(), pr.getClosedAt());
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Date getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Date closedAt) {
    this.closedAt = closedAt;
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }
}
