public class Init extends UserlandProcess {
    @Override
    public void main() {
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.interactive);
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.interactive);
        while (true) {
            try { Thread.sleep(50); } catch (Exception e) { }
            cooperate();
        }
    }
}
