package net.opmasterleo.license.internal.http;

final class OutcomeReportThread extends Thread {

    private final String url;
    private final String requestBody;
    private final String userAgent;

    OutcomeReportThread(String url, String requestBody, String userAgent) {
        this.url = url;
        this.requestBody = requestBody;
        this.userAgent = userAgent;
        setDaemon(true);
        setName("oplicense-outcome-report");
    }

    @Override
    public void run() {
        LicenseHttp.executeOnce(url, requestBody, userAgent, 5_000, 5_000);
    }
}
