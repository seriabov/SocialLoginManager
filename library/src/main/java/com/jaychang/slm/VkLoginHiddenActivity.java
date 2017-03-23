package com.jaychang.slm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiUser;
import com.vk.sdk.api.model.VKList;

/**
 * @author Sergei Riabov
 */

public class VkLoginHiddenActivity extends AppCompatActivity {

  private static final String EXTRA_APP_ID = "app_id";
  private static final String FIELDS = "photo, photo_200, city, sex";

  public static Intent createIntent(Context context, int appId) {
    Intent intent = new Intent(context, VkLoginHiddenActivity.class);
    intent.putExtra(EXTRA_APP_ID, appId);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    VKSdk.customInitialize(getApplicationContext(), getIntent().getIntExtra(EXTRA_APP_ID, 0), null);
    VKSdk.login(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
      @Override
      public void onResult(final VKAccessToken res) {
        VKApi.users()
            .get(VKParameters.from(
                VKApiConst.USER_ID, res.userId,
                VKApiConst.ACCESS_TOKEN, res.accessToken,
                VKApiConst.FIELDS, FIELDS))
            .executeWithListener(new VKRequest.VKRequestListener() {
              @Override
              @SuppressWarnings("unchecked")
              public void onComplete(VKResponse response) {
                VKList<VKApiUser> result = (VKList<VKApiUser>) response.parsedModel;
                if (! result.isEmpty()) {
                  onLoginSuccess(res, result.get(0));
                }
              }

              @Override
              public void onError(VKError error) {
                onLoginError(new Exception(error.errorMessage));
              }
            });

      }
      @Override
      public void onError(VKError error) {
        onLoginError(new Exception(error.errorMessage));
      }
    })) {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  void onLoginSuccess(VKAccessToken res, VKApiUser vkUser) {
    SocialUser user = new SocialUser();
    user.userId = res.userId;
    user.accessToken = res.accessToken;
    user.photoUrl = vkUser.photo_200;
    SocialUser.Profile profile = new SocialUser.Profile();
    profile.email = res.email != null ? res.email : "";
    profile.name =  vkUser.first_name + " " + vkUser.last_name;
    profile.pageLink = "";
    user.profile = profile;
    SocialLoginManager.getInstance(this).onLoginSuccess(user);
    finish();
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
