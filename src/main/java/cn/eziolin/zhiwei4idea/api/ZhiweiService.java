package cn.eziolin.zhiwei4idea.api;

import cn.eziolin.zhiwei4idea.api.model.Card;
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
  Try<String> login(@NotNull String domain, @NotNull String userName, @NotNull String password);

  @NotNull
  Try<String> getCookie(@NotNull String domain);

  /**
   * 通过关键字搜索卡片列表
   *
   * @param keyword 卡片标题/描述/卡号等信息
   * @return 卡片列表数据
   */
  @NotNull
  Try<List<Card>> findCardList(@NotNull String domain, @NotNull String keyword);
}
