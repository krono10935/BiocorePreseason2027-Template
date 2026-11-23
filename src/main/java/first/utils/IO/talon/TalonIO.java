package first.utils.IO.talon;

import com.ctre.phoenix6.controls.CoastOut;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.PositionDutyCycle;
import com.ctre.phoenix6.controls.StaticBrake;
import com.ctre.phoenix6.controls.TorqueCurrentFOC;
import com.ctre.phoenix6.controls.VelocityDutyCycle;
import com.ctre.phoenix6.hardware.TalonFX;

import first.utils.IO.MotorIO;

public class TalonIO extends MotorIO {
    private final static int IMPORTANT_SIGNAL_FREQUENCY = 50;

    protected TalonFX talon;

    public TalonIO(TalonConfig config){
        talon = new TalonFX(config.id, config.canBus);
        talon.getConfigurator().apply(config.talonFXConfig);

        talon.optimizeBusUtilization();

        talon.getPosition().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);
        talon.getVelocity().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);
        talon.getMotorVoltage().setUpdateFrequency(IMPORTANT_SIGNAL_FREQUENCY);



        withAngularPositionControl(
            (double positionRotations) -> talon.setControl(new PositionDutyCycle(positionRotations))
        );
        
        withAngularVelocityControl(
            (double positionRotations) -> talon.setControl(new VelocityDutyCycle(positionRotations))
        );
        
        withBrakeMode(
            () -> talon.setControl(new StaticBrake())
        );
        
        withCoastMode(
            () -> talon.setControl(new CoastOut())
        );
        
        withDutyCycleControl(
            (double dutyCycleOut) -> talon.setControl(new DutyCycleOut(dutyCycleOut))
        );
        
        withTorqueControl(
            (double current) -> talon.setControl(new TorqueCurrentFOC(current))
        ); 
    }

    @Override
    public void updateInputs(MotorInputs inputs){
        inputs.angularPositionRotations = talon.getPosition().getValueAsDouble();
        inputs.angularVelocityRotationsPerSecond = talon.getVelocity().getValueAsDouble();
        inputs.appliedVolts = talon.getMotorVoltage().getValueAsDouble();
        inputs.connected = talon.isConnected();
        inputs.statorCurrentAmps = talon.getStatorCurrent().getValueAsDouble();
        inputs.supplyCurrentAmps = talon.getSupplyCurrent().getValueAsDouble();
    }

}
