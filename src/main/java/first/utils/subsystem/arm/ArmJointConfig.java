package first.utils.subsystem.arm;


public class ArmJointConfig {
    
    public enum JointType{
        PITCH,
        YAW,
        ROLL;
    }

    public double mass = 0;
    public double length = 0;
    public JointType jointType = JointType.PITCH;
}
