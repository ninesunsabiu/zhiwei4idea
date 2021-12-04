package cn.eziolin.zhiwei4idea.setting.model;

public class PluginConfig {
  private String myUsername;
  private String myPassword;
  private String myCookie;

  public String getUsername() {
    return myUsername;
  }

  public void setUsername(String username) {
    this.myUsername = username;
  }

  public String getPassword() {
    return myPassword;
  }

  public void setPassword(String password) {
    this.myPassword = password;
  }

  public String getCookie() {
    return myCookie;
  }

  public void setCookie(String cookie) {
    this.myCookie = cookie;
  }
}
