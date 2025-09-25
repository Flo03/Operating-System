public class Init extends UserlandProcess {
    @Override
    public void main() {
        // Always add an Idle so the scheduler keeps making progress
        OS.CreateProcess(new IdleProcess(), OS.PriorityType.background);

        // Create test processes of different priorities
        OS.CreateProcess(new HelloWorld(), OS.PriorityType.interactive);
        OS.CreateProcess(new GoodbyeWorld(), OS.PriorityType.background);

        // Example "real-time" process that sleeps to avoid demotion
        OS.CreateProcess(new UserlandProcess() {
            @Override public void main() {
                while (true) {
                    System.out.println("[RT] rendering frame");
                    OS.Sleep(60);  // voluntary block; avoids demotion
                    cooperate();
                }
            }
        }, OS.PriorityType.realtime);

        // Init exits per rubric
        OS.Exit();
    }
}
