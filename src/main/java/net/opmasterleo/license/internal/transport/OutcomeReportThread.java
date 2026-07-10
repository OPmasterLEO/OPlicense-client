package net.opmasterleo.license.internal.transport;

final class OutcomeReportThread extends Thread {

    private final String url;
    private final String requestBody;

    OutcomeReportThread(String url, String requestBody) {
        this.url = url;
        this.requestBody = requestBody;
        setDaemon(true);
        setName("oplicense-outcome-report");
    }

    @Override
    public void run() {
        LicenseHttpTransport.executePostFireAndForget(
                url,
                requestBody,
                LicenseHttpTransport.outcomeConnectTimeoutMs(),
                LicenseHttpTransport.outcomeReadTimeoutMs()
        );
    }
}
