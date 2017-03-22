package com.jaychang.slm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import ru.ok.android.sdk.Odnoklassniki;
import ru.ok.android.sdk.OkAuthListener;
import ru.ok.android.sdk.util.OkAuthType;
import ru.ok.android.sdk.util.OkScope;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author Sergei Riabov
 */

public class OkLoginHiddenActivity
    extends AppCompatActivity {
  protected static final String REDIRECT_URL_PREFIX = "okauth://ok";

  private static final String EXTRA_APP_ID = "app_id";
  private static final String EXTRA_APP_KEY = "app_key";

  Odnoklassniki odnoklassniki;

  public static Intent createIntent(Context context, String appId, String appKey) {
    Intent intent = new Intent(context, OkLoginHiddenActivity.class);
    intent.putExtra(EXTRA_APP_ID, appId);
    intent.putExtra(EXTRA_APP_KEY, appKey);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String appId = getIntent().getStringExtra(EXTRA_APP_ID);
    String appKey = getIntent().getStringExtra(EXTRA_APP_KEY);

    Odnoklassniki.createInstance(this, appId, appKey);
    odnoklassniki = Odnoklassniki.getInstance();

    odnoklassniki.requestAuthorization(
        this, REDIRECT_URL_PREFIX.concat(appId), OkAuthType.ANY, OkScope.VALUABLE_ACCESS, OkScope.LONG_ACCESS_TOKEN);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (Odnoklassniki.getInstance().isActivityRequestOAuth(requestCode)) {
      Odnoklassniki.getInstance().onAuthActivityResult(requestCode, resultCode, data, getAuthListener());
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @NonNull
  private OkAuthListener getAuthListener() {
    return new OkAuthListener() {
      @Override
      public void onSuccess(final JSONObject json) {
        try {
          getUserInfo(json.getString("access_token"));
        } catch (JSONException e) {
          onLoginError(e);
        }
      }

      @Override
      public void onError(String error) {
        onLoginError(new Exception(error));
      }

      @Override
      public void onCancel(String error) {
        onLoginCancel();
      }
    };
  }

  void getUserInfo(final String token) {
    Observable.just(token)
        .flatMap(new Func1<String, Observable<String>>()  {
          @Override
          public Observable<String> call(String s) {
            try {
              return Observable.just(odnoklassniki.request("users.getCurrentUser", null, null));
            } catch (IOException e) {
              return Observable.error(e);
            }
          }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(new Action1<String>() {
          @Override
          public void call(String s) {
            onLoginSuccess(token, s);
          }
        },
          new Action1<Throwable>() {
            @Override
            public void call(Throwable s) {
              onLoginError(s);
            }
          });
  }

  void onLoginSuccess(String token, String userInfoStr) {
    try {
      JSONObject userInfo = new JSONObject(userInfoStr);
      SocialUser user = new SocialUser();
      user.userId = userInfo.getString("uid");
      user.accessToken = token;
      user.photoUrl = userInfo.getString("pic_2");
      SocialUser.Profile profile = new SocialUser.Profile();
      profile.email = "";
      profile.name = userInfo.getString("first_name") + " " + userInfo.getString("last_name");
      user.profile = profile;
      SocialLoginManager.getInstance(this).onLoginSuccess(user);
      finish();

    } catch (JSONException e) {
      onLoginError(e);
    }
  }

  void onLoginError(Throwable t) {
    SocialLoginManager.getInstance(this).onLoginError(t);
    finish();
  }

  void onLoginCancel() {
    SocialLoginManager.getInstance(this).onLoginCancel();
    finish();
  }
}
