package first.utils.IO;

import org.wpilib.hardware.bus.CANPort;

import com.ctre.phoenix6.CANBus;

public class MotorConfig {
    public int id = 0;
    public CANBus canBus = new CANBus(CANPort.CAN_D0);
    public double gearRatio = 0;
}
