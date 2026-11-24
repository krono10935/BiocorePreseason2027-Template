// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedOpModeRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;
import org.wpilib.command3.Scheduler;
import org.wpilib.epilogue.Epilogue;
import org.wpilib.epilogue.Logged;
import org.wpilib.framework.RobotBase;

/**
 * Main robot class. Sets up AdvantageKit logging and runs the command scheduler each loop.
 */
@Logged
public class Robot extends LoggedOpModeRobot {
  /** Where the robot program is running and where its inputs come from. */
  public enum Mode {
    /** Running on a real robot: inputs come from hardware and are logged to a file. */
    REAL,
    /** Running in simulation: inputs come from simulated hardware and are published live. */
    SIM,
    /** Replaying a log file: inputs are read from the log instead of hardware. */
    REPLAY
  }

  /**
   * Mode to use when not running on a real robot. Set to {@link Mode#REPLAY} to replay a log,
   * then run the simulator or the {@code replayWatch} Gradle task.
   */
  private static final Mode SIM_MODE = Mode.SIM;

  /** Mode the program is currently running in. */
  public static final Mode CURRENT_MODE = RobotBase.isReal() ? Mode.REAL : SIM_MODE;

  /**
   * Creates the robot and starts the AdvantageKit logger.
   *
   * <ul>
   *   <li>{@link Mode#REAL}: writes a {@code .wpilog} file to the default robot log folder and
   *       publishes live to NetworkTables.
   *   <li>{@link Mode#SIM}: publishes live to NetworkTables only.
   *   <li>{@link Mode#REPLAY}: runs as fast as possible, reads inputs from the log chosen by
   *       {@link LogFileUtil#findReplayLog()}, and writes the result next to it with a
   *       {@code _sim} suffix.
   * </ul>
   */
  public Robot() {
    Logger.recordMetadata("ProjectName", "BiocorePreseason2027");

    switch (CURRENT_MODE) {
      case REAL -> {
        Logger.addDataReceiver(new WPILOGWriter());
        Logger.addDataReceiver(new NT4Publisher());
      }
      case SIM -> Logger.addDataReceiver(new NT4Publisher());
      case REPLAY -> {
        setUseTiming(false);
        String logPath = LogFileUtil.findReplayLog();
        Logger.setReplaySource(new WPILOGReader(logPath));
        Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
      }
    }

    Logger.start();
  }

  /** Runs the command scheduler and updates Epilogue logging. Called once per loop. */
  @Override
  public void robotPeriodic() {
    Scheduler.getDefault().run();
    Epilogue.update(this);
  }
}
