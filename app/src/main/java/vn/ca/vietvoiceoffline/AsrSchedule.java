package vn.ca.vietvoiceoffline;

/** Subtitle-first mode still samples ASR periodically and never skips uncovered audio. */
final class AsrSchedule {
 private long lastCheck=Long.MIN_VALUE;
 boolean shouldRecognize(boolean enabled,boolean covered,long now) {
  if(!enabled||!covered||lastCheck==Long.MIN_VALUE||now-lastCheck>=15000||now<lastCheck) {lastCheck=now;return true;}
  return false;
 }
}
