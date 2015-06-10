package nl.xservices.plugins;

import android.content.Intent;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

public class LaunchMyApp extends CordovaPlugin {

    private static final String ACTION_CHECKINTENT = "checkIntent";
    private static final String PUSH_NOTIFICATION_CLICK = "push_notification_click";
    private static final String TAG = LaunchMyApp.class.getSimpleName();

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (ACTION_CHECKINTENT.equalsIgnoreCase(action)) {

            final Intent intent = ((CordovaActivity) this.webView.getContext()).getIntent();
            final String notificationContent = intent.getStringExtra(PushConstants.EXTRA_NOTIFICATION_CONTENT);
            if (intent.getDataString() != null) {
                Log.d(TAG, "getDataString " + intent.getDataString());
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, intent.getDataString()));
                intent.setData(null);
                return true;
            } else if (notificationContent != null) {
                try {
                    JSONObject jsonObject = new JSONObject(notificationContent);
                    if (PUSH_NOTIFICATION_CLICK.equals(jsonObject.getString("type"))) {
                        Log.d(TAG, "notificationContent " + jsonObject);
                        String url = jsonObject.getString("data");
                        if (url != null && !url.isEmpty()) {
                            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, url.replace("\\", "")));
                            return true;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.removeExtra(PushConstants.EXTRA_NOTIFICATION_CONTENT);
                intent.removeExtra(PushConstants.EXTRA_NOTIFICATION_TITLE);
                return false;
            } else {
                callbackContext.error("App was not started via the launchmyapp URL scheme. Ignoring this errorcallback is the best approach.");
                return false;
            }
        } else {
            callbackContext.error("This plugin only responds to the " + ACTION_CHECKINTENT + " action.");
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        final String intentString = intent.getDataString();
        if (intentString != null && intentString.contains("://")) {
            intent.setData(null);
            try {
                StringWriter writer = new StringWriter(intentString.length() * 2);
                escapeJavaStyleString(writer, intentString, true, false);
                webView.loadUrl("javascript:handleOpenURL('" + writer.toString() + "');");
            } catch (IOException ignore) {
            }
        }
    }

    // Taken from commons StringEscapeUtils
    private static void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote,
                                              boolean escapeForwardSlash) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        }
        if (str == null) {
            return;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                out.write("\\u" + hex(ch));
            } else if (ch > 0xff) {
                out.write("\\u0" + hex(ch));
            } else if (ch > 0x7f) {
                out.write("\\u00" + hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\b':
                        out.write('\\');
                        out.write('b');
                        break;
                    case '\n':
                        out.write('\\');
                        out.write('n');
                        break;
                    case '\t':
                        out.write('\\');
                        out.write('t');
                        break;
                    case '\f':
                        out.write('\\');
                        out.write('f');
                        break;
                    case '\r':
                        out.write('\\');
                        out.write('r');
                        break;
                    default:
                        if (ch > 0xf) {
                            out.write("\\u00" + hex(ch));
                        } else {
                            out.write("\\u000" + hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'':
                        if (escapeSingleQuote) {
                            out.write('\\');
                        }
                        out.write('\'');
                        break;
                    case '"':
                        out.write('\\');
                        out.write('"');
                        break;
                    case '\\':
                        out.write('\\');
                        out.write('\\');
                        break;
                    case '/':
                        if (escapeForwardSlash) {
                            out.write('\\');
                        }
                        out.write('/');
                        break;
                    default:
                        out.write(ch);
                        break;
                }
            }
        }
    }

    private static String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
    }
}