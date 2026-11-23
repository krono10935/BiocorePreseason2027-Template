package first.utils;

import java.util.function.Supplier;

import org.wpilib.math.geometry.Rotation2d;

public class AbsEncoderIO {
    private final Supplier<Rotation2d> read;

    public AbsEncoderIO(Supplier<Rotation2d> read){
        this.read = read;
    }

    public Rotation2d get(){
        return read.get();
    }
}
