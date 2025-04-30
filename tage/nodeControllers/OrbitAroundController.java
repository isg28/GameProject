package tage.nodeControllers;

import tage.*;
import org.joml.*;

public class OrbitAroundController extends NodeController {
    private Vector3f center;
    private float orbitRadius;
    private float orbitSpeed;
    private float angle = 0f;

    private float bobbingHeight = 0.05f;
    private float bobbingSpeed = 0.005f;
    private float bobbingPhase = 0f;

    public OrbitAroundController(Vector3f center, float radius, float speed, GameObject target) {
        super(); 
        this.center = center;
        this.orbitRadius = radius;
        this.orbitSpeed = speed;
        addTarget(target);
    }
    

    @Override
    public void apply(GameObject go) {
        float elapsedTime = getElapsedTime();
        angle += orbitSpeed * elapsedTime;
        bobbingPhase += bobbingSpeed * elapsedTime;

        float x = center.x + orbitRadius * (float)java.lang.Math.cos(angle);
        float z = center.z + orbitRadius * (float)java.lang.Math.sin(angle);
        float y = center.y + bobbingHeight * (float)java.lang.Math.sin(bobbingPhase);

        go.setLocalTranslation(new Matrix4f().translation(x, y, z));
    }
}
