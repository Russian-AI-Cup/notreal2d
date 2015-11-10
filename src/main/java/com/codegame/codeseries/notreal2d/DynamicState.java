package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class DynamicState extends StaticState {
    private Vector2D velocity;
    private Vector2D medianVelocity;
    private Vector2D force;

    private double angularVelocity;
    private double medianAngularVelocity;
    private double torque;

    public DynamicState() {
        this.velocity = new Vector2D(0.0D, 0.0D);
        this.medianVelocity = new Vector2D(0.0D, 0.0D);
        this.force = new Vector2D(0.0D, 0.0D);
    }

    public DynamicState(
            Point2D position, Vector2D velocity, Vector2D force, double angle, double angularVelocity, double torque) {
        super(position, angle);

        this.velocity = velocity.copy();
        this.force = force.copy();
        this.angularVelocity = angularVelocity;
        this.torque = torque;
    }

    public DynamicState(DynamicState state) {
        super(state);

        this.velocity = state.velocity.copy();
        this.force = state.force.copy();
        this.angularVelocity = state.angularVelocity;
        this.torque = state.torque;
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity.copy();
    }

    public Vector2D getMedianVelocity() {
        return medianVelocity;
    }

    public void setMedianVelocity(Vector2D medianVelocity) {
        this.medianVelocity = medianVelocity;
    }

    public Vector2D getForce() {
        return force;
    }

    public void setForce(Vector2D force) {
        this.force = force.copy();
    }

    public double getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(double angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public double getMedianAngularVelocity() {
        return medianAngularVelocity;
    }

    public void setMedianAngularVelocity(double medianAngularVelocity) {
        this.medianAngularVelocity = medianAngularVelocity;
    }

    public double getTorque() {
        return torque;
    }

    public void setTorque(double torque) {
        this.torque = torque;
    }
}
