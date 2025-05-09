public class ProgressIndicator {
    long size;
    long count;
    Pair completedPortionOfJob;
    long lastProgressDisplayTime;

    long INTERVAL_BETWEEN_DISPLAY_REFRESH_NANO_SECONDS = (long) 1e9;

    String explainer;

    public ProgressIndicator(long size, long count, String explainer) {
        this.size = size;
        this.count = count;
        this.completedPortionOfJob = new Pair(0, 0);
        this.lastProgressDisplayTime = 0;
        this.explainer = explainer;
    }

    public void tick(long bytesCopied) {
        completedPortionOfJob.size += bytesCopied;
        completedPortionOfJob.count += 1;
        long currTime = System.nanoTime();
        if (currTime > lastProgressDisplayTime + INTERVAL_BETWEEN_DISPLAY_REFRESH_NANO_SECONDS) {
            long filesC = completedPortionOfJob.count;
            long filesT = count;
            long bytesC = completedPortionOfJob.size;
            long bytesT = size;
            String countPercent = Utils.nicePercent(filesC * 1.0 / filesT);
            String bytesPercent = Utils.nicePercent(bytesC * 1.0 / bytesT);
            printProgress(filesC, countPercent, bytesC, bytesPercent);
            lastProgressDisplayTime = currTime;
        }
    }

    public void done() {
        printProgress(count, "100.00%", size, "100.00%");
    }

    public void printProgress(long filesC, String countPercent, long bytesC, String bytesPercent) {
        System.out.println("Progress " + Utils.timestamp() + ": " + filesC + " " + explainer + " (" + countPercent + "), " + Utils.formatSize(bytesC) + " (" + bytesPercent + ")");
        // TODO MiB/s
        // TODO ETA
        // TODO separately report data copied vs total data in repo
    }
}
