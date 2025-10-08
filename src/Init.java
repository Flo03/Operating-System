public class Init extends UserlandProcess {
    @Override
    public void main() {
        OS.CreateProcess(new IdleProcess(), OS.PriorityType.background);
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.interactive);
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.background);

        OS.CreateProcess(new UserlandProcess() {
            @Override public void main() {
                while (true) {
                    System.out.println("[RT] rendering frame");
                    OS.Sleep(60);
                    cooperate();
                }
            }
        }, OS.PriorityType.realtime);

        OS.Exit();
    }
}
