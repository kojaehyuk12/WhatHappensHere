package io.github.mygame;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen(this)); // 'this'를 전달하여 Game 객체를 전달합니다.
    }
}
