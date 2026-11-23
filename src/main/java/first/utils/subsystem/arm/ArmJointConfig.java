package first.utils.subsystem.arm;

import org.wpilib.math.geometry.Rotation3d;

public class ArmJointConfig {
    
    public enum JointType{
        PITCH(Rotation3d.fromDegrees(0, 90, 0)),
        YAW(Rotation3d.fromDegrees(0, 0, 90)),
        ROLL(Rotation3d.fromDegrees(90, 0, 0));

        public final Rotation3d direction;

        JointType(Rotation3d direction){
            this.direction = direction;
        }
    }

    public double mass = 0;
    public double length = 0;
    public JointType jointType = JointType.PITCH;
}
