package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.api.model.OpenAPICard;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.jetbrains.annotations.NotNull;

public interface ZhiweiService {
  /**
   * 通过用户名和密码登录知微
   *
   * @param userName 用户名
   * @param password 密码
   * @return 登录成功后返回 session cookie
   */
  @NotNull
  Try<String> login(String domain, String userName, String password);

  @NotNull
  Try<String> getCookie(String domain);

  /**
   * 通过关键字搜索卡片列表
   *
   * @param keyword 卡片标题/描述/卡号等信息
   * @return 卡片列表数据
   */
  @NotNull
  Try<List<Card>> findCardList(String domain, String keyword);

  /**
   * 通过 id 查看 id 背后所代表的实体
   *
   * @param id 实体标记
   * @return 实体记录
   */
  @NotNull
  Try<String> searchIdForEverything(PluginConfig config, String id);

  /** 通过卡片的 code 搜索卡片 */
  @NotNull
  Try<Set<OpenAPICard>> searchCardByCode(PluginConfig config, Set<String> codeSet);

  /** 根据卡片 ID 集合 批量移动到解决验证状态 */
  @NotNull
  Try<Set<Either<String, String>>> batchUpdateFields(
      PluginConfig config, Set<String> cardIdSet, Object field);
}
