import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.*;

import graphics.Shader;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;

import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import input.Input;
import level.Level;
import maths.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.nio.IntBuffer;

public class Main implements Runnable {

	private boolean running = false;
	private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
	private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();

	private long window;

	private Level level;
	ImFont font1;

	private float ar = 1.f;

	public void start() {
		running = true;
		Thread thread = new Thread(this, "Game");
		thread.start();
	}

	private void init() {
		if (!glfwInit()) {
			System.err.println("Could not initialize GLFW!");
			return;
		}

		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
		
		int width = 1280, height = 720;
		window = glfwCreateWindow(width, height, "Flappy Bird", NULL, NULL);
		
		if (window == NULL) {
			System.err.println("Could not create GLFW window!");
			return;
		}

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		assert vidmode != null;
		
		glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		ar = (float)vidmode.width() / (float)vidmode.height();

		glfwSetKeyCallback(window, new Input());
		glfwMakeContextCurrent(window);
		glfwShowWindow(window);
		
		GL.createCapabilities();

		ImGui.createContext();
		var io = ImGui.getIO();
		ImGui.styleColorsDark();
		imGuiGlfw.init(window, true);
		font1 = io.getFonts().addFontFromFileTTF("fonts/CascadiaCode.ttf", 42);
		imGuiGl3.init("#version 130");

		glEnable(GL_DEPTH_TEST);
		glActiveTexture(GL_TEXTURE1);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		System.out.println("OpenGL: " + glGetString(GL_VERSION));
		Shader.loadAll();

		Matrix4f pr_matrix = Matrix4f.orthographic(-10.0f, 10.0f, -10.0f, 10.0f, -1.0f, 1.0f);
		Shader.BG.setUniformMat4f("pr_matrix", pr_matrix);
		Shader.BG.setUniform1i("tex", 1);

		Shader.BIRD.setUniformMat4f("pr_matrix", pr_matrix);
		Shader.BIRD.setUniform1i("tex", 1);

		Shader.PIPE.setUniformMat4f("pr_matrix", pr_matrix);
		Shader.PIPE.setUniform1i("tex", 1);

		level = new Level(ar);
	}

	private void destroy(){
		imGuiGlfw.dispose();
		imGuiGl3.dispose();
		ImGui.destroyContext();
		glfwDestroyWindow(window);
		glfwTerminate();
	}

	public void run() {
		init();

		IntBuffer w = BufferUtils.createIntBuffer(1);
		IntBuffer h = BufferUtils.createIntBuffer(1);

		while (running) {
			glfwGetWindowSize(window, w, h);
			glViewport(0, 0, (int)(w.get(0)), (int)(h.get(0)));
			update();
			render();
			if (glfwWindowShouldClose(window)) running = false;
		}

		destroy();
	}

	private void update() {
		glfwPollEvents();
		level.update();
		if (level.isGameOver()) {
			level = new Level(ar);
		}
	}

	private void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		level.render();

		int error = glGetError();
		if (error != GL_NO_ERROR)
			System.out.println(error);

		imGuiGlfw.newFrame();
		ImGui.newFrame();
		if (ImGui.begin("Score", new ImBoolean(true), ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoDecoration)) {
			ImGui.pushFont(font1);
			ImGui.text("Score: " + level.getScore());
			ImGui.popFont();
			ImGui.end();
		}
		ImGui.render();
		imGuiGl3.renderDrawData(ImGui.getDrawData());

		glfwSwapBuffers(window);
	}

	public static void main(String[] args) {
		new Main().start();
	}

}
