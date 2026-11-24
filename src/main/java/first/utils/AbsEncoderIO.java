package first.utils;

import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.AutoLog;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.util.MathUtil;

/**
 * Hardware-agnostic wrapper around an absolute encoder that converts raw encoder rotations into a
 * zeroed, optionally inverted, wrapped mechanism position.
 *
 * <p>The conversion performed by {@link #get()} is:
 *
 * <ol>
 *   <li>Scale the raw reading by {@code fullRange} to get mechanism rotations.
 *   <li>Subtract {@code expectedZero} so the configured zero reads as {@code 0.0}.
 *   <li>Negate the result if {@code inverted} is set.
 *   <li>Wrap the result into the window {@code [minPosition, minPosition + fullRange]}.
 * </ol>
 *
 * <p>Because the output is wrapped modulo {@code fullRange}, the raw supplier may report any
 * contiguous one-rotation window (e.g. {@code [-0.5, 0.5)} or {@code [0, 1)}); the result is the
 * same either way.
 *
 * <p>For AdvantageKit replay, read the encoder through {@link #updateInputs(AbsEncoderInputs)},
 * log the inputs, and convert the logged value with {@link #fromRaw(double)} rather than calling
 * {@link #get()}, which always reads the hardware.
 */
public class AbsEncoderIO {

    /**
     * Logged telemetry inputs for an absolute encoder.
     */
    @AutoLog
    public static class AbsEncoderInputs {
        /** Unprocessed encoder position in encoder rotations, as returned by {@link AbsEncoderIO#getRaw()}. */
        public double rawRotations = 0.0;

        /** Creates an inputs struct with a raw reading of zero. */
        public AbsEncoderInputs() {}
    }

    private final DoubleSupplier read;
    private final double fullRange;   // Mechanism rotations per 1 encoder rotation (1.0 / encoderToMechanismRatio)
    private final double minPosition; // Lowest valid mechanism position before wrapping (in mechanism rotations)
    private final boolean inverted;
    private double expectedZero;      // Scaled reading (in mechanism rotations) where get() should return 0.0

    /**
     * Creates a centered, non-inverted encoder mounted 1:1 with the mechanism.
     *
     * <p>Output is wrapped into {@code [-0.5, 0.5]} mechanism rotations, with zero at a raw reading
     * of {@code 0.0}.
     *
     * @param read supplier returning the raw absolute encoder position in rotations
     */
    public AbsEncoderIO(DoubleSupplier read) {
        this(read, 1.0, 0.0, false, -0.5);
    }

    /**
     * Creates a centered, geared encoder whose output is wrapped into
     * {@code [-fullRange / 2, fullRange / 2]} mechanism rotations.
     *
     * @param read         supplier returning the raw absolute encoder position in rotations
     * @param fullRange    mechanism rotations per one full encoder rotation (e.g. {@code 1.0 / 8.0}
     *                     when the encoder spins 8 times per mechanism rotation); must be positive
     * @param expectedZero scaled reading (raw rotations times {@code fullRange}) at mechanism zero
     * @param inverted     {@code true} to reverse the positive direction around zero
     * @throws IllegalArgumentException if {@code fullRange} is not positive
     */
    public AbsEncoderIO(DoubleSupplier read, double fullRange, double expectedZero, boolean inverted) {
        this(read, fullRange, expectedZero, inverted, -fullRange / 2.0);
    }

    /**
     * Full constructor supporting asymmetric travel ranges (e.g. shooter hoods or single-sided arms),
     * where the wrap seam must sit outside the mechanism's physical travel.
     *
     * @param read         supplier returning the raw absolute encoder position in rotations
     * @param fullRange    mechanism rotations per one full encoder rotation (e.g. {@code 1.0 / 8.0}
     *                     when the encoder spins 8 times per mechanism rotation); must be positive
     * @param expectedZero scaled reading (raw rotations times {@code fullRange}) at mechanism zero
     * @param inverted     {@code true} to reverse the positive direction around zero
     * @param minPosition  lowest reported mechanism position in rotations, i.e. where the wrap seam
     *                     sits; output is wrapped into {@code [minPosition, minPosition + fullRange]}
     * @throws IllegalArgumentException if {@code fullRange} is not positive
     */
    public AbsEncoderIO(
            DoubleSupplier read,
            double fullRange,
            double expectedZero,
            boolean inverted,
            double minPosition) {
        if (fullRange <= 0.0) {
            throw new IllegalArgumentException("fullRange must be positive.");
        }
        this.read = read;
        this.fullRange = fullRange;
        this.expectedZero = expectedZero;
        this.inverted = inverted;
        this.minPosition = minPosition;
    }

    /**
     * Returns the zeroed, direction-corrected mechanism position.
     *
     * @return mechanism position in rotations, wrapped into
     *     {@code [minPosition, minPosition + fullRange]}
     */
    public double get() {
        return fromRaw(read.getAsDouble());
    }

    /**
     * Converts a raw encoder reading into a zeroed, direction-corrected mechanism position, using
     * the same conversion as {@link #get()}. Use this with a logged
     * {@link AbsEncoderInputs#rawRotations} so the result is reproducible in replay.
     *
     * @param rawRotations raw encoder position in encoder rotations
     * @return mechanism position in rotations, wrapped into
     *     {@code [minPosition, minPosition + fullRange]}
     */
    public double fromRaw(double rawRotations) {
        double pos = rawRotations * fullRange - expectedZero;
        if (inverted) {
            pos = -pos;
        }
        return MathUtil.inputModulus(pos, minPosition, minPosition + fullRange);
    }

    /**
     * Reads the encoder into {@code inputs}.
     *
     * @param inputs the telemetry struct to populate
     */
    public void updateInputs(AbsEncoderInputs inputs) {
        inputs.rawRotations = read.getAsDouble();
    }

    /**
     * Returns the mechanism position from {@link #get()} as a {@link Rotation2d}.
     *
     * @return mechanism position as a rotation
     */
    public Rotation2d getRotation2d() {
        return Rotation2d.fromRotations(get());
    }

    /**
     * Returns the unprocessed encoder reading, with no scaling, zero offset, inversion, or wrapping
     * applied. Useful for logging and for finding the value to pass to
     * {@link #setExpectedZero(double)}.
     *
     * @return raw encoder position in encoder rotations
     */
    public double getRaw() {
        return read.getAsDouble();
    }

    /**
     * Sets the current physical position as the new mechanism zero, so {@link #get()} returns
     * {@code 0.0} at this position.
     *
     * <p>This only changes the in-memory offset; it is lost on restart. Read
     * {@link #getExpectedZero()} afterwards and store it as a constant to make it permanent.
     */
    public void zero() {
        this.expectedZero = read.getAsDouble() * fullRange;
    }

    /**
     * Sets the scaled reading at which the mechanism is considered to be at zero.
     *
     * @param expectedZero scaled reading (raw rotations times {@code fullRange}) at mechanism zero
     */
    public void setExpectedZero(double expectedZero) {
        this.expectedZero = expectedZero;
    }

    /**
     * Returns the scaled reading at which the mechanism is considered to be at zero.
     *
     * @return scaled reading (raw rotations times {@code fullRange}) at mechanism zero
     */
    public double getExpectedZero() {
        return expectedZero;
    }
}
