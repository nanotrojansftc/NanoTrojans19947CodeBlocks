/*
 * Copyright (c) 2022 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package teamcode.NanoTrojansAuto;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvWebcam;

import teamcode.OpenCVExt.LSideConeLocDetection;
import teamcode.controls_NanoTrojans;
import teamcode.drive.SampleMecanumDrive;
import teamcode.trajectorysequence.TrajectorySequence;



/**
 * This class contains the Autonomous Mode program.
 */
@Config
@Autonomous(name = "Auto_BlueClose_OpenCV")
public class NanoTorjanAuto_BlueRight_OpenCV extends LinearOpMode {

    // Constants for encoder counts and wheel measurements
    static final double COUNTS_PER_REVOLUTION = 537.7; // Encoder counts per revolution
    static final double WHEEL_DIAMETER_MM = 96.0; // Wheel diameter in millimeters
    static final double MM_PER_REVOLUTION = WHEEL_DIAMETER_MM * Math.PI; // Wheel circumference
    static final double COUNTS_PER_MM = COUNTS_PER_REVOLUTION / MM_PER_REVOLUTION; // Counts per millimeter
    static final double COUNTS_PER_INCH = COUNTS_PER_MM * 25.4; // Counts per inch
    OpenCvWebcam webcam2;
    LSideConeLocDetection pipeline2;
    LSideConeLocDetection.LSideConePosition position = LSideConeLocDetection.LSideConePosition.OTHER;
    private DcMotor frontLeftMotor;
    private DcMotor frontRightMotor;
    private DcMotor rearLeftMotor;
    private DcMotor rearRightMotor;
    private Servo clawLift = null;
    private Servo armLift = null;
    private Servo clawLeft = null;
    private Servo clawRight = null;
    private DcMotor lsRight = null;
    private DcMotor lsLeft = null;
    //private DcMotor intake = null;
    private CRServo planeLaunch = null;
    private CRServo robotLift = null;
    private int frontLeftMotorCounts = 0;


    //The following are for single camera
    private int frontRightMotorCounts = 0;
    private int rearLeftMotorCounts = 0;
    private int rearRightMotorCounts = 0;
    private controls_NanoTrojans g2control;


    public static double parkingLongStrafe = 30;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize motors
        frontLeftMotor = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRightMotor = hardwareMap.get(DcMotor.class, "frontRight");
        rearLeftMotor = hardwareMap.get(DcMotor.class, "backLeft");
        rearRightMotor = hardwareMap.get(DcMotor.class, "backRight");
        lsRight = hardwareMap.dcMotor.get("lsRight");
        lsLeft = hardwareMap.dcMotor.get("lsLeft");
        //intake = hardwareMap.dcMotor.get("intake");

        //Servo Motors
        planeLaunch = hardwareMap.crservo.get("planeLaunch");
        robotLift = hardwareMap.crservo.get("robotLift");

        //hang


        // get 2 claw motors
        clawLeft = hardwareMap.servo.get("clawLeft");
        clawRight = hardwareMap.servo.get("clawRight");

        // get 2 arm motors
        clawLift = hardwareMap.servo.get("clawLift");
        armLift = hardwareMap.servo.get("armLift");



        // Set motor directions (adjust as needed based on your robot configuration)
        frontLeftMotor.setDirection(DcMotor.Direction.FORWARD);
        frontRightMotor.setDirection(DcMotor.Direction.REVERSE);
        rearLeftMotor.setDirection(DcMotor.Direction.FORWARD);
        rearRightMotor.setDirection(DcMotor.Direction.REVERSE);

        // Set motor modes
        setRunMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);

        /*
         *  Initialize camera and set pipeline
         */
        int cameraMonitorViewId2 = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webcam2 = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam 1"), cameraMonitorViewId2);
        pipeline2 = new LSideConeLocDetection();
        webcam2.setPipeline(pipeline2);
        g2control=new controls_NanoTrojans( lsRight, lsLeft, planeLaunch,
                clawLeft, clawRight, clawLift, armLift, robotLift);

        /*
         *  Create a thread for camera, so it will watch for us
         */
        webcam2.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webcam2.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode) {
            }
        });

        /*
         *  create an instacne for MecanumDrive car
         */
        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);

        waitForStart();
        boolean stop = false;


        while (opModeIsActive() && !stop) {
//            telemetry.addData("Analysis", pipeline2.getPosition());
//            telemetry.update();
            g2control.closeClaw();
            g2control.clawUp();

            // Don't burn CPU cycles busy-looping in this sample
            //sleep(1000);
            position = pipeline2.getPosition();
            telemetry.addData("Blue Close Got position", position);
            telemetry.update();
            if (position == LSideConeLocDetection.LSideConePosition.LEFT) {
//                telemetry.addData("Analysis", pipeline2.getPosition());
//                telemetry.update();
                TrajectorySequence trajSeq = drive.trajectorySequenceBuilder(new Pose2d())
                        .strafeLeft(13)
                        .forward(25)
                        .back(10)
                        .turn(Math.toRadians(-90))
                        .forward(29)
                        .strafeRight(12)
                        .build();
                drive.followTrajectorySequence(trajSeq);
                doRestStuff();
                TrajectorySequence trajSeq2 = drive.trajectorySequenceBuilder(new Pose2d())
                        .strafeLeft(32)
                        .build();
                drive.followTrajectorySequence(trajSeq2);

                stop = true;
            } else if (position == LSideConeLocDetection.LSideConePosition.CENTER) {
//                telemetry.addData("Analysis", pipeline2.getPosition());
//                telemetry.update();
                 /*
                 *  push the pixel to the middle line and back a little bit and
                 */
                TrajectorySequence trajSeq = drive.trajectorySequenceBuilder(new Pose2d())
                        .forward(34)
                        .back(8)    //Going throught the middle door to booard
                        .turn(Math.toRadians(-90))
                        .forward(38)
                        .strafeRight(3)
                        .build();
                drive.followTrajectorySequence(trajSeq);
                doRestStuff();
                TrajectorySequence trajSeq2 = drive.trajectorySequenceBuilder(new Pose2d())
                        .strafeLeft(35)
                        .build();
                drive.followTrajectorySequence(trajSeq2);

                stop = true;

            } else if (position == LSideConeLocDetection.LSideConePosition.RIGHT) {
//                telemetry.addData("Analysis", pipeline2.getPosition());
//                telemetry.update();
                TrajectorySequence trajSeq = drive.trajectorySequenceBuilder(new Pose2d())
                        .forward(33)
                        .turn(Math.toRadians(90))
                        .forward(8)
                        .back(10)
                        .turn(Math.toRadians(-90))
                        .turn(Math.toRadians(-90))
                        .forward(36)
                        .strafeRight(18)
                        .build();
                drive.followTrajectorySequence(trajSeq);
                doRestStuff();
                TrajectorySequence trajSeq2 = drive.trajectorySequenceBuilder(new Pose2d())
                        .strafeLeft(50)
                        .build();
                drive.followTrajectorySequence(trajSeq2);

                stop = true;
            }
        }
    }

    /*
     * move up liner slides
     */
    private void moveUpLSLow() {
        //move up linear slides
        lsRight.setPower(-1);
        lsLeft.setPower(1);
        sleep(250);
        lsRight.setPower(0);
        lsLeft.setPower(0);
        //end move up

    }

    private void liftArm() {
//        armLift.setPosition(0.8);
    }

    private void doRestStuff() {
        //************************
        // Lift claw and setup position
        g2control.smallls();
        sleep(250);
        g2control.smalllsstop();
        //end move up
        g2control.armFull();
        sleep(500);
        g2control.clawUp();
        sleep(3500);
        g2control.openClaw();
        sleep(3000);

        g2control.reversesmallls();
        sleep(250);
        g2control.reversesmalllsstop();
        sleep(1000);


        g2control.closeClaw();

        sleep(1000);
        g2control.clawUp();
        sleep(1000);
        g2control.closeClaw();
        g2control.armUp();
        sleep(1500);
        g2control.armDown();
        sleep(250);
        g2control.clawUp();
        sleep(1000);
        //g2control.openClaw();

    }

    private void setRunMode(DcMotor.RunMode mode) {
        frontLeftMotor.setMode(mode);
        frontRightMotor.setMode(mode);
        rearLeftMotor.setMode(mode);
        rearRightMotor.setMode(mode);
    }


    private void moveDistance(double inches, double power) {
        int targetPosition = (int) (inches * COUNTS_PER_INCH);

        frontLeftMotor.setTargetPosition(frontLeftMotor.getCurrentPosition() + targetPosition);
        frontRightMotor.setTargetPosition(frontRightMotor.getCurrentPosition() + targetPosition);
        rearLeftMotor.setTargetPosition(rearLeftMotor.getCurrentPosition() + targetPosition);
        rearRightMotor.setTargetPosition(rearRightMotor.getCurrentPosition() + targetPosition);


        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        //double power = 0.3; // Adjust power as needed
        frontLeftMotor.setPower(power);
        frontRightMotor.setPower(power);
        rearLeftMotor.setPower(power);
        rearRightMotor.setPower(power);

        while (opModeIsActive() &&
                frontLeftMotor.isBusy() &&
                frontRightMotor.isBusy() &&
                rearLeftMotor.isBusy() &&
                rearRightMotor.isBusy()) {
            // Wait for motors to reach target position
        }

        resetEncoderCounts();
        resetRobotPosition();
        stopRobot();
    }


    private void turnLeft90D(double power) {
        int turnCounts = calculateTurnCountsLeft();

        // Set target positions for motors to perform a 90-degree right turn
        frontLeftMotor.setTargetPosition(frontLeftMotor.getCurrentPosition() + turnCounts);
        frontRightMotor.setTargetPosition(frontRightMotor.getCurrentPosition() - turnCounts);
        rearLeftMotor.setTargetPosition(rearLeftMotor.getCurrentPosition() + turnCounts);
        rearRightMotor.setTargetPosition(rearRightMotor.getCurrentPosition() - turnCounts);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        //double power = 0.8; // Adjust power as needed for turning
        frontLeftMotor.setPower(power);
        frontRightMotor.setPower(power);
        rearLeftMotor.setPower(power);
        rearRightMotor.setPower(power);

        while (opModeIsActive() &&
                frontLeftMotor.isBusy() &&
                frontRightMotor.isBusy() &&
                rearLeftMotor.isBusy() &&
                rearRightMotor.isBusy()) {
            // Wait for motors to reach target position

            //telemetry.addData(" Parallel Right Encoder Current Position",parallel2.getCurrentPosition());
        }

        resetEncoderCounts();
        resetRobotPosition();
        stopRobot();
    }


    private void turnRight90D(double power) {
        int turnCounts = calculateTurnCountsRight();

        // Set target positions for motors to perform a 90-degree right turn
        frontLeftMotor.setTargetPosition(frontLeftMotor.getCurrentPosition() - turnCounts);
        frontRightMotor.setTargetPosition(frontRightMotor.getCurrentPosition() + turnCounts);
        rearLeftMotor.setTargetPosition(rearLeftMotor.getCurrentPosition() - turnCounts);
        rearRightMotor.setTargetPosition(rearRightMotor.getCurrentPosition() + turnCounts);

        setRunMode(DcMotor.RunMode.RUN_TO_POSITION);

        //double power = 0.5; // Adjust power as needed for turning
        frontLeftMotor.setPower(power);
        frontRightMotor.setPower(power);
        rearLeftMotor.setPower(power);
        rearRightMotor.setPower(power);

        while (opModeIsActive() &&
                frontLeftMotor.isBusy() &&
                frontRightMotor.isBusy() &&
                rearLeftMotor.isBusy() &&
                rearRightMotor.isBusy()) {
            // Wait for motors to reach target position
        }

        stopRobot();
        resetEncoderCounts();
        resetRobotPosition();
    }

    private void strafeRight(double inches, double power) {
        int targetPosition = (int) (inches * COUNTS_PER_INCH);

        frontLeftMotor.setTargetPosition(frontLeftMotor.getCurrentPosition() - targetPosition);
        frontRightMotor.setTargetPosition(frontRightMotor.getCurrentPosition() - targetPosition);
        rearLeftMotor.setTargetPosition(rearLeftMotor.getCurrentPosition() + targetPosition);
        rearRightMotor.setTargetPosition(rearRightMotor.getCurrentPosition() + targetPosition);

        frontLeftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rearLeftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rearRightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        frontLeftMotor.setPower(power);
        frontRightMotor.setPower(power);
        rearLeftMotor.setPower(power);
        rearRightMotor.setPower(power);

        while (opModeIsActive() && frontLeftMotor.isBusy() && frontRightMotor.isBusy() &&
                rearLeftMotor.isBusy() && rearRightMotor.isBusy()) {
            // Wait until motors reach target position
        }

        frontLeftMotor.setPower(0);
        frontRightMotor.setPower(0);
        rearLeftMotor.setPower(0);
        rearRightMotor.setPower(0);

        frontLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void strafeLeft(double inches, double power) {
        int targetPosition = (int) (inches * COUNTS_PER_INCH);

        frontLeftMotor.setTargetPosition(frontLeftMotor.getCurrentPosition() + targetPosition);
        frontRightMotor.setTargetPosition(frontRightMotor.getCurrentPosition() + targetPosition);
        rearLeftMotor.setTargetPosition(rearLeftMotor.getCurrentPosition() - targetPosition);
        rearRightMotor.setTargetPosition(rearRightMotor.getCurrentPosition() - targetPosition);

        frontLeftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rearLeftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rearRightMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        frontLeftMotor.setPower(power);
        frontRightMotor.setPower(power);
        rearLeftMotor.setPower(power);
        rearRightMotor.setPower(power);

        while (opModeIsActive() && frontLeftMotor.isBusy() && frontRightMotor.isBusy() &&
                rearLeftMotor.isBusy() && rearRightMotor.isBusy()) {
            // Wait until motors reach target position
        }

        frontLeftMotor.setPower(0);
        frontRightMotor.setPower(0);
        rearLeftMotor.setPower(0);
        rearRightMotor.setPower(0);

        frontLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private int calculateTurnCountsLeft() {
        // Calculate encoder counts needed for a 90-degree turn based on robot-specific measurements
        // Example calculation: Assume each motor needs to move half of the circumference of a circle with a 12-inch radius
        double robotWidth = 28; // This value represents half the distance between the wheels
        double wheelCircumference = Math.PI * robotWidth;
        double countsPerInch = COUNTS_PER_INCH; // Use your previously calculated value
        return (int) ((wheelCircumference / 4.0) * countsPerInch); // 90-degree turn for each wheel
    }

    private int calculateTurnCountsRight() {
        // Calculate encoder counts needed for a 90-degree turn based on robot-specific measurements
        // Example calculation: Assume each motor needs to move half of the circumference of a circle with a 12-inch radius
        double robotWidth = 27.5; // This value represents half the distance between the wheels
        double wheelCircumference = Math.PI * robotWidth;
        double countsPerInch = COUNTS_PER_INCH; // Use your previously calculated value
        return (int) ((wheelCircumference / 4.0) * countsPerInch); // 90-degree turn for each wheel
    }

    private void stopRobot() {
        frontLeftMotor.setPower(0);
        frontRightMotor.setPower(0);
        rearLeftMotor.setPower(0);
        rearRightMotor.setPower(0);
        setRunMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void resetEncoderCounts() {
        // Reset the encoder counts for all four motors to zero
        frontLeftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rearLeftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rearRightMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        frontLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearLeftMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rearRightMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void resetRobotPosition() {
        // Reset any other variables or mechanisms used for position tracking or orientation
        frontLeftMotorCounts = 0;
        frontRightMotorCounts = 0;

        rearLeftMotorCounts = 0;
        rearRightMotorCounts = 0;
        // For encoder-based position tracking, resetting the counts is sufficient in this example
    }
}
