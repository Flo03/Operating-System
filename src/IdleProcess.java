public class IdleProcess extends UserlandProcess {
    @Override
    public void main() {
        while (true) {
            cooperate(); // don't Sleep here
        }
    }
}
