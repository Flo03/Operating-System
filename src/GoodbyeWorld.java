public class GoodbyeWorld extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            System.out.println("Goodbye World");
            OS.Sleep(50);   // exercise Sleep/wakeup path
            cooperate();
        }
    }
}
