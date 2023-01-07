package ru.rzn.gmyasoedov.gmaven;


import java.nio.file.Path;

public class Test {

    public static void main(String[] args) {

        Path of = Path.of("../a/1.txt");
        Path normalize = of.normalize();
        Path path = normalize.toAbsolutePath();
        System.out.println(path);
    }

}
