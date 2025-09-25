public class IdleProcess extends UserlandProcess {
    @Override public void main() {
        while (true) {
            cooperate();  // keep the kernel ticking; DO NOT Sleep here
        }
    }
}
