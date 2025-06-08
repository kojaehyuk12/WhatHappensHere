package io.github.mygame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class Joystick extends Actor {
    public Texture baseTexture;
    public Texture knobTexture;
    private Vector2 center;
    private Vector2 knobPosition;
    private float radius;
    private boolean isTouched;
    private boolean enabled; // 활성화 상태

    private Vector2 direction;

    public Joystick(Texture base, Texture knob, float x, float y) {
        this.baseTexture = base;
        this.knobTexture = knob;
        this.setPosition(x, y);
        this.setSize(base.getWidth(), base.getHeight());
        this.center = new Vector2(x + base.getWidth() / 2, y + base.getHeight() / 2);
        this.knobPosition = new Vector2(center.x, center.y);
        this.radius = base.getWidth() / 2;
        this.isTouched = false;
        this.enabled = true; // 기본 활성화 상태
        this.direction = new Vector2(0, 0);

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float touchX, float touchY, int pointer, int button) {
                if (!enabled) return false; // 비활성화 상태에서는 입력 무시
                if (pointer == 0) { // 첫 번째 터치 포인터만 처리
                    isTouched = true;
                    updateKnobPosition(touchX, touchY);
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float touchX, float touchY, int pointer) {
                if (!enabled) return; // 비활성화 상태에서는 입력 무시
                if (isTouched && pointer == 0) {
                    updateKnobPosition(touchX, touchY);
                }
            }

            @Override
            public void touchUp(InputEvent event, float touchX, float touchY, int pointer, int button) {
                if (!enabled) return; // 비활성화 상태에서는 입력 무시
                if (isTouched && pointer == 0) {
                    isTouched = false;
                    knobPosition.set(center);
                    direction.set(0, 0);
                }
            }
        });
    }

    private void updateKnobPosition(float touchX, float touchY) {
        Vector2 touch = new Vector2(getX() + touchX, getY() + touchY);
        Vector2 delta = touch.cpy().sub(center);
        if (delta.len() > radius) {
            delta.nor().scl(radius);
        }
        knobPosition.set(center.cpy().add(delta));
        if (delta.len() > 0) {
            direction.set(delta).nor();
        } else {
            direction.set(0, 0);
        }
    }

    public Vector2 getDirection() {
        return enabled ? direction : new Vector2(0, 0); // 비활성화 상태에서는 0 벡터 반환
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            isTouched = false;
            knobPosition.set(center);
            direction.set(0, 0);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(baseTexture, getX(), getY());
        batch.draw(knobTexture, knobPosition.x - knobTexture.getWidth() / 2, knobPosition.y - knobTexture.getHeight() / 2);
    }

    public void dispose() {
        if (baseTexture != null) {
            baseTexture.dispose();
        }
        if (knobTexture != null) {
            knobTexture.dispose();
        }
    }
}
