public class GoodbyeWorld extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            System.out.println("Goodbye World");
            try { Thread.sleep(50); } catch (Exception e) { }
            cooperate();
        }
    }
}
