package first.utils.subsystem.arm;


/**
 * Physical description of a single {@link ArmJoint} and the link it drives.
 */
public class ArmJointConfig {

    /**
     * Axis a joint rotates about, expressed in the previous link's frame (+X along the link,
     * +Y left, +Z up). Positive angles follow the right-hand rule.
     */
    public enum JointType{
        /**
         * Rotation about +Y. Note that by the right-hand rule a positive angle tilts the link
         * toward -Z (down).
         */
        PITCH,
        /** Rotation about +Z. A positive angle swings the link toward +Y (counter-clockwise from above). */
        YAW,
        /**
         * Rotation about +X (along the link). Does not move this link's own endpoint, but rotates
         * every link after it.
         */
        ROLL;
    }

    /** Mass of the link in kilograms. Not yet used by the kinematics. */
    public double mass = 0;

    /** Length of the link from this joint's axis to the next joint (or end effector), in meters. */
    public double length = 0;

    /** Axis this joint rotates about. */
    public JointType jointType = JointType.PITCH;

    /** Creates a config with zero mass and length and a {@link JointType#PITCH} joint. */
    public ArmJointConfig() {}
}
