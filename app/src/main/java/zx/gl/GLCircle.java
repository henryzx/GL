package zx.gl;

import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by zhengxiao on 09/09/2017.
 */

public class GLCircle extends ContextWrapper implements GLSurfaceView.Renderer {

  interface Callback{
    void requestRender();
  }

  private static final String LTAG = "ZCircle";

  public final PointF centerf;
  private final float maxRadi;
  private final Bitmap bitmap;
  private final Callback callback;

  private float[] radi = {0.0f, 0.0f, 0.0f};

  private int mProgramObject;
  private int mTexID;
  private FloatBuffer mVertices;
  private ShortBuffer mTexCoords;
  private final float[] mVerticesData = {-0.5f, -0.5f, 0, 0.5f, -0.5f, 0, -0.5f, 0.5f, 0, 0.5f,
      0.5f, 0};
  private final short[] mTexCoordsData = {0, 1, 1, 1, 0, 0, 1, 0};
  private boolean mBfirst = false;
  private float[] opacity = {0.0f, 0.0f, 0.0f};
  private int width;
  private int height;
  private float ratio;

  private final float[] mMVPMatrix = new float[16];
  private final float[] mProjectionMatrix = new float[16];
  private final float[] mViewMatrix = new float[16];


  public GLCircle(Context context, PointF centerf, Bitmap icon, float maxRadi, Callback callback) {
    super(context);
    this.centerf = centerf;
    this.maxRadi = maxRadi;
    this.bitmap = icon;
    this.callback = callback;

  }

  private CopyOnWriteArrayList<ValueAnimator> animators = new CopyOnWriteArrayList<>();
  private TimeAnimator timeAnimator;

  public void start() {
    stop();
    DecelerateInterpolator sharedInterpolator = new DecelerateInterpolator();
    {
      ValueAnimator animator = ValueAnimator.ofFloat(0.0f, maxRadi);
      animator.setRepeatCount(1000);
      animator.setDuration(3000L);
      animator.setInterpolator(sharedInterpolator);
      animators.add(animator);
    }
    {
      ValueAnimator animator = ValueAnimator.ofFloat(0.0f, maxRadi);
      animator.setRepeatCount(1000);
      animator.setDuration(3000L);
      animator.setStartDelay(500L);
      animator.setInterpolator(sharedInterpolator);
      animators.add(animator);
    }
    {
      ValueAnimator animator = ValueAnimator.ofFloat(0.0f, maxRadi);
      animator.setRepeatCount(1000);
      animator.setDuration(3000L);
      animator.setStartDelay(1000L);
      animator.setInterpolator(sharedInterpolator);
      animators.add(animator);
    }

    timeAnimator = new TimeAnimator();
    timeAnimator.setTimeListener(new TimeListener() {
      @Override
      public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
        for (int i = 0; i < animators.size(); i++) {
          ValueAnimator animator = animators.get(i);
          radi[i] = ((float) animator.getAnimatedValue());
          opacity[i] = (1f - animator.getAnimatedFraction());
        }
        callback.requestRender();

      }
    });

    for (ValueAnimator animator : animators) {
      animator.start();
    }
    timeAnimator.start();

  }

  public void stop() {
    for (ValueAnimator animator : animators) {
      animator.end();
    }
    animators.clear();
    if (timeAnimator != null) {
      timeAnimator.end();
      timeAnimator = null;
    }
  }

  private void renderCircle(PointF centerf, float radi, float opacity) {

    PointF p1f = new PointF(centerf.x, centerf.y);
    p1f.offset(radi, radi);
//    p1f.offset(radi, radi * ratio);
    PointF p2f = new PointF(centerf.x, centerf.y);
    p2f.offset(-radi, -radi);
//    p2f.offset(-radi, -radi * ratio);

    float mVerticesData[] = new float[]{p1f.x, p1f.y, 0.0f, p2f.x, p1f.y, 0.0f, p1f.x,
        p2f.y, 0.0f, p2f.x, p2f.y, 0.0f};
    mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();
    mVertices.put(mVerticesData).position(0);

    mTexCoords = ByteBuffer.allocateDirect(mTexCoordsData.length * 2)
        .order(ByteOrder.nativeOrder()).asShortBuffer();
    mTexCoords.put(mTexCoordsData).position(0);
    if (!mBfirst) {
      comipleShaderAndLinkProgram();
      loadTexture();
      mBfirst = true;
    }

    GLES20.glUseProgram(mProgramObject);

    // apply matrix
    mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramObject, "uMVPMatrix");
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

    GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, mVertices);
    GLES20.glEnableVertexAttribArray(0);

    GLES20.glVertexAttribPointer(1, 2, GLES20.GL_SHORT, false, 0, mTexCoords);
    GLES20.glEnableVertexAttribArray(1);

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);

    GLES20.glEnable(GLES20.GL_BLEND);
    int opacityHandle = GLES20.glGetUniformLocation(mProgramObject, "Opacity");
    if (opacityHandle != -1) {
      GLES20.glUniform1f(opacityHandle, opacity);
    }

    //Blending.
    GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

    GLES20.glDisable(GLES20.GL_BLEND);


  }

  private int mMVPMatrixHandle;
  private void comipleShaderAndLinkProgram() {
    final String vShaderStr = "uniform mat4 uMVPMatrix;"
        + "attribute vec4 a_position;    \n"
        + "attribute vec2 a_texCoords; \n"
        + "varying vec2 v_texCoords; \n"
        + "void main()                  \n"
        + "{                            \n"
        + "   gl_Position = uMVPMatrix * a_position;  \n"
        + "    v_texCoords = a_texCoords; \n"
        + "}                            \n";
    final String fShaderStr = "precision mediump float;                     \n"
        + "uniform sampler2D u_Texture; \n"
        + "uniform float Opacity;       \n"
        + "varying vec2 v_texCoords; \n"
        + "void main()                                  \n"
        + "{                                            \n"
        + "  gl_FragColor = texture2D(u_Texture, v_texCoords) * Opacity ;\n"
//        + "  gl_FragColor.a *= Opacity; ;\n"
        + "}                                            \n";
    int vertexShader;
    int fragmentShader;
    int programObject;
    int[] linked = new int[1];
    // Load the vertex/fragment shaders
    vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vShaderStr);
    fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fShaderStr);
    // Create the program object
    programObject = GLES20.glCreateProgram();
    if (programObject == 0) {
      return;

    }

    GLES20.glAttachShader(programObject, vertexShader);
    GLES20.glAttachShader(programObject, fragmentShader);
    // Bind vPosition to attribute 0
    GLES20.glBindAttribLocation(programObject, 0, "a_position");
    GLES20.glBindAttribLocation(programObject, 1, "a_texCoords");
    // Link the program
    GLES20.glLinkProgram(programObject);
    // Check the link status
    GLES20.glGetProgramiv(programObject, GLES20.GL_LINK_STATUS, linked, 0);
    if (linked[0] == 0) {
      Log.e(LTAG, "Error linking program:");
      Log.e(LTAG, GLES20.glGetProgramInfoLog(programObject));
      GLES20.glDeleteProgram(programObject);
      return;
    }
    mProgramObject = programObject;
  }

  private int loadShader(int shaderType, String shaderSource) {
    int shader;
    int[] compiled = new int[1];
    // Create the shader object
    shader = GLES20.glCreateShader(shaderType);
    if (shader == 0) {
      return 0;

    }
    // Load the shader source
    GLES20.glShaderSource(shader, shaderSource);
    // Compile the shader
    GLES20.glCompileShader(shader);
    // Check the compile status
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0) {
      Log.e(LTAG, GLES20.glGetShaderInfoLog(shader));
      GLES20.glDeleteShader(shader);
      return 0;
    }
    return shader;
  }

  public void setCenterByTouch(float x, float y) {
    float[] unitM = new float[] {1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
    int[] viewM = new int[]{0,0,width,height};
    float[] objM = new float[4];
    int result = GLU.gluUnProject(x, height-y, 0, unitM, 0, mMVPMatrix, 0, viewM, 0, objM, 0);

    centerf.x = objM[0];
    centerf.y = objM[1];

  }

  private void loadTexture() {
    if (bitmap != null) {
      int[] texID = new int[1];
      GLES20.glGenTextures(1, texID, 0);
      GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID[0]);
      mTexID = texID[0];
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
      GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

//      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
//      GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

//      GLES20
//          .glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//      GLES20
//          .glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

      GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
    }
  }

  @Override
  public void onSurfaceCreated(GL10 gl, EGLConfig config) {
    GLES20.glClearColor(0f,0f,0f,0f);
    mBfirst = false;
  }



  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height) {
    this.width = width;
    this.height = height;
    this.ratio = (float) width / height;
    GLES20.glViewport(0, 0, width, height);
    Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
  }

  @Override
  public void onDrawFrame(GL10 gl) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    // Set the camera position (View matrix)
    Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

    // Calculate the projection and view transformation
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

    for (int i = 0; i < 3; i++) {
      renderCircle(centerf, radi[i], opacity[i]);
    }
  }
}
