package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.Card;
import cn.eziolin.zhiwei4idea.setting.model.PluginConfig;
import io.vavr.collection.List;
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
}
