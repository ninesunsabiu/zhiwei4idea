package cn.eziolin.zhiwei4idea.gitpush;

import cn.eziolin.zhiwei4idea.ramda.RamdaUtil;
import cn.eziolin.zhiwei4idea.setting.ConfigSettingsState;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.update.DisposableUpdate;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.impl.TimedVcsCommitImpl;
import com.intellij.vcs.log.impl.VcsCommitMetadataImpl;
import git4idea.GitCommit;
import git4idea.GitLocalBranch;
import git4idea.history.GitHistoryUtils;
import git4idea.push.GitPushSupport;
import git4idea.push.GitPushTarget;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.repo.GitRepositoryManager;
import io.vavr.CheckedFunction0;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class IssueCardCloser
    implements GitRepositoryChangeListener, Notifications, Disposable {

  private final @NotNull Project myProject;
  private final @NotNull MergingUpdateQueue myQueue;
  private final @NotNull Object LOCK = new Object();
  private final @NotNull Set<GitRepository> myDirtyReposWithOutgoing = new java.util.HashSet<>();
  private final @NotNull Map<GitRepository, List<Tuple2<GitLocalBranch, List<GitCommit>>>>
      myLocalBranchesWithOutgoing = new ConcurrentHashMap<>();
  private @Nullable MessageBusConnection myConnection;

  public IssueCardCloser(@NotNull Project project) {
    myProject = project;
    myQueue =
        new MergingUpdateQueue(
            "GitPushListener", 1000, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);
  }

  private static boolean shouldCheckIncomingOutgoing() {
    return AdvancedSettings.getBoolean("git.update.incoming.outgoing.info");
  }

  private void updateBranchesWithOutgoing() {
    if (shouldCheckIncomingOutgoing()) {
      synchronized (LOCK) {
        myDirtyReposWithOutgoing.addAll(
            GitRepositoryManager.getInstance(myProject).getRepositories());
      }
      scheduleUpdate();
    }
  }

  public void activate() {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              Option.of(myProject.isDisposed())
                  .filterNot(it -> it)
                  .forEach(
                      ignored -> {
                        Option.of(myConnection)
                            .onEmpty(
                                () -> {
                                  myConnection = myProject.getMessageBus().connect(this);
                                  // ??????????????? plugin.xml ??????????????????
                                  // ??????????????????????????? ???????????????????????? listener ????????? stateless ???
                                  myConnection.subscribe(GitRepository.GIT_REPO_CHANGE, this);
                                  myConnection.subscribe(Notifications.TOPIC, this);
                                });
                        updateBranchesWithOutgoing();
                      });
            });
  }

  @Override
  public void repositoryChanged(@NotNull GitRepository repository) {
    if (!shouldCheckIncomingOutgoing()) return;
    synchronized (LOCK) {
      myDirtyReposWithOutgoing.add(repository);
    }
    scheduleUpdate();
  }

  /**
   * ???????????? git4idea.push.GitPushResultNotification ????????? ?????? push ???????????????
   *
   * @param notification ????????????
   */
  @Override
  public void notify(@NotNull Notification notification) {
    // ?????? GitPushResultNotification ?????? public ??? ?????????????????? instanceof ???????????????
    // ??????????????? ?????? groupId ?????????
    var matchedGroupId = VcsNotifier.STANDARD_NOTIFICATION.getDisplayId();
    Option.of(notification)
        .filter(
            it -> {
              // ?????????????????????????????????????????????
              // git4idea ??????????????????????????????????????????
              // ??????????????? GitBundle.message("push.notification.description.pushed") ??????????????????
              return it.getGroupId().equals(matchedGroupId)
                  && it.getContent().startsWith("<b>Pushed ");
            })
        .forEach(
            ignored -> {
              var allMessages =
                  List.ofAll(myLocalBranchesWithOutgoing.values())
                      .flatMap(Function.identity())
                      .flatMap(tuple -> tuple._2)
                      .distinctBy(TimedVcsCommitImpl::getId);

              movingCardStatusToVerification(allMessages);

              myLocalBranchesWithOutgoing.clear();
            });
  }

  @Override
  public void dispose() {}

  private void scheduleUpdate() {
    myQueue.queue(
        DisposableUpdate.createDisposable(
            this,
            "calculate outgoing",
            () -> {
              java.util.List<GitRepository> withOutgoing;
              synchronized (LOCK) {
                withOutgoing = new ArrayList<>(myDirtyReposWithOutgoing);
                myDirtyReposWithOutgoing.clear();
              }
              withOutgoing.forEach(
                  r -> {
                    Option.of(calculateBranchesWithOutgoing(r))
                        .filter(List::nonEmpty)
                        .forEach(
                            (list) -> {
                              myLocalBranchesWithOutgoing.put(r, list);
                            });
                  });
            }));
  }

  private List<Tuple2<GitLocalBranch, List<GitCommit>>> calculateBranchesWithOutgoing(
      GitRepository repository) {
    var branchesCollection = repository.getBranches();
    return List.ofAll(repository.getBranches().getLocalBranches())
        .map(
            branch -> {
              var pushTarget = Option.of(GitPushSupport.getPushTargetIfExist(repository, branch));
              Option<Hash> localHashForRemoteBranch =
                  pushTarget.map(GitPushTarget::getBranch).map(branchesCollection::getHash);
              var commitList =
                  localHashForRemoteBranch
                      .map(remoteHash -> Tuple.of(remoteHash, branchesCollection.getHash(branch)))
                      .map(
                          hashTuple -> {
                            var remoteHash = hashTuple._1;
                            var localHash = hashTuple._2;
                            var proj = repository.getProject();
                            var gitRootFile = repository.getRoot();
                            var diffString = remoteHash.asString() + ".." + localHash.asString();
                            CheckedFunction0<List<GitCommit>> getCommitList =
                                () ->
                                    List.ofAll(
                                        GitHistoryUtils.history(proj, gitRootFile, diffString));

                            return Try.of(getCommitList).getOrElseGet(ignored -> List.empty());
                          })
                      .getOrElse(List::empty);

              return Tuple.of(branch, commitList);
            })
        .filter(tuple -> tuple._2.nonEmpty());
  }

  private void movingCardStatusToVerification(List<GitCommit> list) {
    var codeGroupKey = "code";

    var keywordsRegExp =
        "@ag:((close[sd]?)|(fix(es|ed)?)|(resolve[sd]?))\\s#(?<" + codeGroupKey + ">\\d+)";
    var pattern = Pattern.compile(keywordsRegExp, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    list.map(VcsCommitMetadataImpl::getFullMessage)
        .flatMap(
            message -> {
              // Java ?????????????????? matchAll ?????????
              // ??????????????? while ???
              var mather = pattern.matcher(message);
              java.util.HashSet<String> set = new java.util.HashSet<>();
              while (mather.find()) {
                set.add(mather.group(codeGroupKey));
              }
              return List.ofAll(set);
            })
        .distinct()
        .transform(
            it -> it.isEmpty() ? Option.<HashSet<String>>none() : Option.some(HashSet.ofAll(it)))
        .peek(
            codeSet -> {
              ApplicationManager.getApplication()
                  .invokeLater(
                      () -> {
                        RamdaUtil.getZhiweiService
                            .get()
                            .forEach(
                                service -> {
                                  var configValidation =
                                      ConfigSettingsState.getPluginConfigSafe("tkb")
                                          .toValidation(() -> "????????????????????????");

                                  configValidation
                                      .map(
                                          config -> {
                                            return service
                                                .searchCardByCode(config, codeSet)
                                                .map(
                                                    allCard -> {
                                                      // ?????????????????????
                                                      var allowMovingStatus =
                                                          HashSet.of(
                                                              // ??????
                                                              "f425ec772bd1457ead1807b868ddc924",
                                                              // ??????
                                                              "ef06cbbedf6c4ecaacddbb6678dc996a",
                                                              // ?????????
                                                              "33348c31b213447cb6d9f090f40e009c");
                                                      return allCard
                                                          .filter(
                                                              card ->
                                                                  allowMovingStatus.contains(
                                                                      card.currentStatusId))
                                                          .map(card -> card.id);
                                                    })
                                                .flatMapTry(
                                                    movingCardIdSet -> {
                                                      var targetStatusId =
                                                          "f56f4622f1c34246883f7af994ad9728";
                                                      var value =
                                                          new Map[] {
                                                            java.util.Map.ofEntries(
                                                                Map.entry("id", targetStatusId))
                                                          };
                                                      Object field =
                                                          java.util.Map.ofEntries(
                                                              Map.entry("flag", "status"),
                                                              Map.entry("type", "STATUS"),
                                                              Map.entry("value", value));

                                                      return service.batchUpdateFields(
                                                          config, movingCardIdSet, field);
                                                    });
                                          })
                                      .flatMap(v -> v.toValidation(Throwable::getMessage))
                                      .fold(
                                          error -> {
                                            return RamdaUtil.showMessage
                                                .andThen(ignored -> RamdaUtil.ignore.get())
                                                .apply(
                                                    "Zhiwei",
                                                    "??????????????????: \n" + error + "\n",
                                                    NotificationType.INFORMATION);
                                          },
                                          successSet -> {
                                            return successSet
                                                .foldLeft(
                                                    Tuple.of(
                                                        HashSet.<String>empty(),
                                                        HashSet.<String>empty()),
                                                    (acc, item) -> {
                                                      return item.fold(
                                                          left ->
                                                              Tuple.of(acc._1.add(left), acc._1),
                                                          right ->
                                                              Tuple.of(acc._1, acc._2.add(right)));
                                                    })
                                                .apply(
                                                    (failedIdSet, successIdSet) -> {
                                                      Supplier<String> failMessage =
                                                          () -> {
                                                            return failedIdSet.isEmpty()
                                                                ? ""
                                                                : "????????????????????????: \n"
                                                                    + failedIdSet.mkString("\t")
                                                                    + "\n";
                                                          };
                                                      return RamdaUtil.showMessage
                                                          .andThen(
                                                              ignored -> RamdaUtil.ignore.get())
                                                          .apply(
                                                              "Zhiwei",
                                                              "????????????: "
                                                                  + successIdSet.size()
                                                                  + "??????\n"
                                                                  + successIdSet.mkString("\t")
                                                                  + "\n"
                                                                  + failMessage.get(),
                                                              NotificationType.INFORMATION);
                                                    });
                                          });
                                });
                      });
            });
  }
}
