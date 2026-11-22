package com.geomatter.lull;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lull: a full-window wrapper around the Slack web app for Meta Quest.
 * Not affiliated with or endorsed by Slack Technologies.
 */
public class MainActivity extends Activity {

    private static final String START_URL = "https://app.slack.com/client";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    // The release workflow publishes version.txt (the build number) next to each APK.
    private static final String LATEST_VERSION_URL =
            "https://github.com/GeoMatter/Lull/releases/latest/download/version.txt";
    private static final String RELEASES_PAGE_URL =
            "https://github.com/GeoMatter/Lull/releases/latest";

    // Domains that stay inside the app. Everything else opens in the Quest browser.
    private static final String[] INTERNAL_DOMAINS = {
            "slack.com", "slack-edge.com", "slack-files.com", "slack-imgs.com", "slackb.com"
    };

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#1A1D21"));
        setContentView(webView);

        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            Uri incoming = getIntent() != null ? getIntent().getData() : null;
            webView.loadUrl(incoming != null && isInternal(incoming) ? incoming.toString() : START_URL);
            checkForUpdate();
            showVersion();
        }
    }

    /** Shows the installed version for a couple of seconds on launch. */
    private void showVersion() {
        Toast.makeText(this, "Lull " + BuildConfig.VERSION_NAME + " (build " + BuildConfig.VERSION_CODE + ")",
                Toast.LENGTH_SHORT).show();
    }

    /** Quietly checks GitHub for a newer build and offers it. Any failure is ignored. */
    private void checkForUpdate() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(LATEST_VERSION_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setInstanceFollowRedirects(true);
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return;

                String text;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    text = reader.readLine();
                }
                if (text == null) return;
                int latest = Integer.parseInt(text.trim());
                if (latest > BuildConfig.VERSION_CODE) {
                    runOnUiThread(this::showUpdateDialog);
                }
            } catch (Exception ignored) {
                // Offline, no release yet, or bad data: skip silently.
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showUpdateDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Update available")
                .setMessage("A newer version of Lull is out. You have " + BuildConfig.VERSION_NAME
                        + " (build " + BuildConfig.VERSION_CODE + "). Update it through SideQuest, or download it from the release page.")
                .setPositiveButton("View release", (d, w) -> openExternally(Uri.parse(RELEASES_PAGE_URL)))
                .setNegativeButton("Later", null)
                .show();
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        // Slack rejects embedded WebViews, so present as desktop Chrome with the same engine version.
        s.setUserAgentString(desktopUserAgent(s.getUserAgentString()));

        // Keep you signed in between launches.
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) {
                    return false; // embedded frames load normally
                }
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (("https".equals(scheme) || "http".equals(scheme)) && isInternal(uri)) {
                    return false; // load in-app
                }
                if (isGoogleSignIn(uri)) {
                    showGoogleSignInTip(); // Google blocks sign-in inside apps
                    return true;
                }
                openExternally(uri);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        // Hand downloads to the Quest browser, which handles them properly.
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) ->
                openExternally(Uri.parse(url)));
    }

    private static String desktopUserAgent(String defaultUa) {
        String chromeVersion = "131.0.0.0";
        Matcher m = Pattern.compile("Chrome/([\\d.]+)").matcher(defaultUa);
        if (m.find()) {
            chromeVersion = m.group(1);
        }
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/"
                + chromeVersion + " Safari/537.36";
    }

    private static boolean isGoogleSignIn(Uri uri) {
        String host = uri.getHost();
        return host != null && host.equalsIgnoreCase("accounts.google.com");
    }

    private void showGoogleSignInTip() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Use email to sign in")
                .setMessage("Google sign-in doesn't work inside apps. Type your Gmail address in the email box and continue with email instead. It's the same account.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private static boolean isInternal(Uri uri) {
        String host = uri.getHost();
        if (host == null) return false;
        host = host.toLowerCase();
        for (String domain : INTERNAL_DOMAINS) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    private void openExternally(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            // No app can open it; ignore.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri data = intent.getData();
        if (data != null && isInternal(data)) {
            webView.loadUrl(data.toString());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
