public class lerpTest {

    public static void main(String[] args) {
        test mainClass = new test();

        System.out.println(mainClass.lerp(0, 120, 0.8f));
        System.out.println(mainClass.lerp(80, 140, 0.8f));

        System.out.println(mainClass.bilinearInterpolate(
                0,
                120,
                80,
                140,
                0.8f, 0.3f));
    }

}
