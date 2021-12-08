package cn.eziolin.zhiwei4idea.api;

import java.util.function.Supplier;

public class ZhiweiApiError extends Error {
  private ZhiweiApiError() {
    super();
  }

  private ZhiweiApiError(String message) {
    super(message);
  }

  private ZhiweiApiError(Throwable throwable) {
    super(throwable);
  }

  private ZhiweiApiError(String message, Throwable throwable) {
    super(message, throwable);
  }

  public static Supplier<ZhiweiApiError> make(String message) {
    return () -> new ZhiweiApiError(message);
  }

  public static Supplier<ZhiweiApiError> make(Throwable throwable) {
    return () -> new ZhiweiApiError(throwable);
  }

  public static Supplier<ZhiweiApiError> make(String message, Throwable throwable) {
    return () -> new ZhiweiApiError(message, throwable);
  }

  public static ZhiweiApiError of(String message) {
    return new ZhiweiApiError(message);
  }

  public static ZhiweiApiError of(Throwable throwable) {
    return new ZhiweiApiError(throwable);
  }

  public static ZhiweiApiError of(String message, Throwable throwable) {
    return new ZhiweiApiError(message, throwable);
  }
}
