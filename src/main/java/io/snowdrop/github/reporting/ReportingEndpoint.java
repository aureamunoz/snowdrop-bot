/**
 * Copyright 2018 The original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
**/

package io.snowdrop.github.reporting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Observable;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Stream;
import io.snowdrop.Status;
import io.snowdrop.github.reporting.model.Issue;
import io.snowdrop.github.reporting.model.PullRequest;
import io.snowdrop.github.reporting.model.Repository;
import io.vertx.core.json.JsonObject;

@Path("/reporting")
public class ReportingEndpoint {

    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy");

    @Inject
    GithubReportingService service;

    @GET
    @Path("/issues/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<Status> streamIssuesStatus() {
        return service.getIssueStatuses();
    }

    @GET
    @Path("/prs/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Publisher<Status> streamPullRequestsStatus() {
        return service.getPullrequests();
    }


    @GET
    @Path("/enable")
    public void enable() {
        service.enable();
    }

    @GET
    @Path("/disable")
    public void disable() {
        service.disable();
    }

    @GET
    @Path("/status")
    public boolean status() {
        return service.status();
    }

    @GET
    @Path("/collect/issues")
    public void collectIssues() {
        service.collectIssues();
    }

    @GET
    @Path("/collect/pull-requests")
    public void collectPullRequests() {
        service.collectPullRequests();
    }

    @GET
    @Path("/orgs")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> organizations() {
        return service.getRepositoryCollector().getOrganizations();
    }

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> users() {
        return service.getRepositoryCollector().getUsers();
    }

    @GET
    @Path("/repositories")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Set<Repository>> repositories() {
        return service.getRepositoryCollector().getRepositories();
    }

    @GET
    @Path("/repositories/{user}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Repository> repositories(@PathParam("user") String user) {
        return service.getRepositoryCollector().getRepositories().get(user);
    }

    @GET
    @Path("/start-time")
    @Produces(MediaType.APPLICATION_JSON)
    public Long recomendedStartDate() {
        return Date.from(service.getPullRequestCollector().getStartTime().toInstant()).getTime();
    }

    @GET
    @Path("/end-time")
    @Produces(MediaType.APPLICATION_JSON)
    public Long recomendedEndDate() {
        return Date.from(service.getPullRequestCollector().getEndTime().toInstant()).getTime();
    }

    @GET
    @Path("/data/pr")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Set<PullRequest>> pullRequestData(@QueryParam("startTime") String startTimeString,
            @QueryParam("endTime") String endTimeString) throws ParseException {
        Map<String, Set<PullRequest>> map = new HashMap<>();
        Date startTime = startTimeString != null ? DF.parse(startTimeString)
            : Date.from(service.getPullRequestCollector().getStartTime().toInstant());
        Date endTime = endTimeString != null ? DF.parse(endTimeString)
                : Date.from(service.getPullRequestCollector().getEndTime().toInstant());
        Set<PullRequest> pullRequests = service.getPullRequestCollector().getPullRequests()
                .filter(p -> p.isActiveDuring(startTime, endTime)).collect(Collectors.toSet());
        map.put("data", pullRequests);
        return map;
    }

    @GET
    @Path("/data/issues")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Set<Issue>> issueData(@QueryParam("startTime") String startTimeString,
            @QueryParam("endTime") String endTimeString) throws ParseException {
        Map<String, Set<Issue>> map = new HashMap<>();
        Date startTime = startTimeString != null ? DF.parse(startTimeString)
                : Date.from(service.getIssueCollector().getStartTime().toInstant());
        Date endTime = endTimeString != null ? DF.parse(endTimeString)
                : Date.from(service.getIssueCollector().getEndTime().toInstant());
        Set<Issue> issues = service.getIssueCollector().getIssues().filter(i -> i.isActiveDuring(startTime, endTime))
                .collect(Collectors.toSet());
        map.put("data", issues);
        return map;
    }

    @GET
    @Path("/pr/repo/{user}/{repo}/user/{creator}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<PullRequest> pullRequests(@PathParam("creator") String creator, @PathParam("user") String user,
            @PathParam("repo") String repo, @QueryParam("startTime") String startTimeString,
            @QueryParam("endTime") String endTimeString) throws ParseException {
        Date startTime = startTimeString != null ? DF.parse(startTimeString)
                : Date.from(service.getPullRequestCollector().getStartTime().toInstant());
        Date endTime = endTimeString != null ? DF.parse(endTimeString)
                : Date.from(service.getPullRequestCollector().getEndTime().toInstant());

        return service.getPullRequestCollector().userPullRequests(creator, Repository.fromFork(creator, user, repo), "all")
                .stream().filter(i -> i.isActiveDuring(startTime, endTime)).collect(Collectors.toSet());
    }

    @GET
    @Path("/issues/repo/{user}/{repo}/user/{assignee}")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Issue> issues(@PathParam("assignee") String assignee, @PathParam("user") String user,
            @PathParam("repo") String repo, @QueryParam("startTime") String startTimeString,
            @QueryParam("endTime") String endTimeString) throws ParseException {
        Date startTime = startTimeString != null ? DF.parse(startTimeString)
                : Date.from(service.getIssueCollector().getStartTime().toInstant());
        Date endTime = endTimeString != null ? DF.parse(endTimeString)
                : Date.from(service.getIssueCollector().getEndTime().toInstant());
        return service.getIssueCollector().userIssues(assignee, Repository.fromFork(assignee, user, repo), "all").stream()
                .filter(i -> i.isActiveDuring(startTime, endTime)).collect(Collectors.toSet());
    }
}
