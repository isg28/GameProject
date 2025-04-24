package tage;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.gl2.GLUT;
import org.joml.*;

/**
* Manages up to two HUD strings, implemented as GLUT strings.
* This class is instantiated automatically by the engine.
* Note that this class utilizes deprectated OpenGL functionality.
* <p>
* The available fonts are:
* <ul>
* <li> GLUT.BITMAP_8_BY_13
* <li> GLUT.BITMAP_9_BY_15
* <li> GLUT.BITMAP_TIMES_ROMAN_10
* <li> GLUT.BITMAP_TIMES_ROMAN_24
* <li> GLUT.BITMAP_HELVETICA_10
* <li> GLUT.BITMAP_HELVETICA_12
* <li> GLUT.BITMAP_HELVETICA_18
* </ul>
* @author Scott Gordon
*/

public class HUDmanager
{	private GLCanvas myCanvas;
	private GLUT glut = new GLUT();
	private Engine engine;

	private String HUD1string, HUD2string, HUD3string, HUD4string, HUD5string, HUD6string, HUD7string, HUD8string, 
									HUD9string, HUD10string, HUD11string, HUD12string;
	private float[] HUD1color, HUD2color, HUD3color, HUD4color, HUD5color, HUD6color, HUD7color, HUD8color, 
									HUD9color, HUD10color, HUD11color, HUD12color;
	private int HUD1font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD2font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD3font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD4font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD5font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD6font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD7font = GLUT.BITMAP_TIMES_ROMAN_24;

	private int HUD8font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD9font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD10font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD11font = GLUT.BITMAP_TIMES_ROMAN_24;
	private int HUD12font = GLUT.BITMAP_TIMES_ROMAN_24;

	private int HUD1x, HUD1y, HUD2x, HUD2y, HUD3x, HUD3y, HUD4x, HUD4y, HUD5x, HUD5y, HUD6x, HUD6y, HUD7x, HUD7y, HUD8x, HUD8y,
											HUD9x, HUD9y, HUD10x, HUD10y, HUD11x, HUD11y, HUD12x, HUD12y;;

	// The constructor is called by the engine, and should not be called by the game application.
	// It initializes the two HUDs to empty strings.

	protected HUDmanager(Engine e)
	{	engine = e;
		HUD1string = "";
		HUD2string = "";
		HUD3string = "";
		HUD4string = ""; 
		HUD5string = ""; 
		HUD6string = ""; 
		HUD7string = ""; 
		HUD8string = ""; 
		HUD9string = ""; 
		HUD10string = ""; 
		HUD11string = ""; 
		HUD12string = ""; 

		HUD1color = new float[3];
		HUD2color = new float[3];
		HUD3color = new float[3];
		HUD4color = new float[3];
		HUD5color = new float[3];
		HUD6color = new float[3];
		HUD7color = new float[3];
		HUD8color = new float[3];
		HUD9color = new float[3];
		HUD10color = new float[3];
		HUD11color = new float[3];
		HUD12color = new float[3];
	}
	
	protected void setGLcanvas(GLCanvas g) { myCanvas = g; }

	protected void drawHUDs()
	{	GL4 gl4 = (GL4) GLContext.getCurrentGL();
		GL4bc gl4bc = (GL4bc) gl4;

		gl4.glUseProgram(0);

		gl4bc.glColor3f(HUD1color[0], HUD1color[1], HUD1color[2]);
		gl4bc.glWindowPos2d (HUD1x, HUD1y);
		glut.glutBitmapString(HUD1font, HUD1string);

		gl4bc.glColor3f(HUD2color[0], HUD2color[1], HUD2color[2]);
		gl4bc.glWindowPos2d (HUD2x, HUD2y);
		glut.glutBitmapString (HUD2font, HUD2string);

		gl4bc.glColor3f(HUD3color[0], HUD3color[1], HUD3color[2]);
		gl4bc.glWindowPos2d (HUD3x, HUD3y);
		glut.glutBitmapString(HUD3font, HUD3string);

		gl4bc.glColor3f(HUD4color[0], HUD4color[1], HUD4color[2]);
		gl4bc.glWindowPos2d (HUD4x, HUD4y);
		glut.glutBitmapString(HUD4font, HUD4string);

		gl4bc.glColor3f(HUD5color[0], HUD5color[1], HUD5color[2]);
		gl4bc.glWindowPos2d (HUD5x, HUD5y);
		glut.glutBitmapString(HUD5font, HUD5string);

		gl4bc.glColor3f(HUD6color[0], HUD6color[1], HUD6color[2]);
		gl4bc.glWindowPos2d (HUD6x, HUD6y);
		glut.glutBitmapString(HUD6font, HUD6string);

		gl4bc.glColor3f(HUD7color[0], HUD7color[1], HUD7color[2]);
		gl4bc.glWindowPos2d (HUD7x, HUD7y);
		glut.glutBitmapString(HUD7font, HUD7string);

		gl4bc.glColor3f(HUD8color[0], HUD8color[1], HUD8color[2]);
		gl4bc.glWindowPos2d (HUD8x, HUD8y);
		glut.glutBitmapString(HUD8font, HUD8string);

		gl4bc.glColor3f(HUD9color[0], HUD9color[1], HUD9color[2]);
		gl4bc.glWindowPos2d (HUD9x, HUD9y);
		glut.glutBitmapString(HUD9font, HUD9string);

		gl4bc.glColor3f(HUD10color[0], HUD10color[1], HUD10color[2]);
		gl4bc.glWindowPos2d (HUD10x, HUD10y);
		glut.glutBitmapString(HUD10font, HUD10string);

		gl4bc.glColor3f(HUD11color[0], HUD11color[1], HUD11color[2]);
		gl4bc.glWindowPos2d (HUD11x, HUD11y);
		glut.glutBitmapString(HUD11font, HUD11string);

		gl4bc.glColor3f(HUD12color[0], HUD12color[1], HUD12color[2]);
		gl4bc.glWindowPos2d (HUD12x, HUD12y);
		glut.glutBitmapString(HUD12font, HUD12string);
	}

	/** sets HUD #1 to the specified text string, color, and location */
	public void setHUD1(String string, Vector3f color, int x, int y)
	{	HUD1string = string;
		HUD1color[0]=color.x(); HUD1color[1]=color.y(); HUD1color[2]=color.z();
		HUD1x = x;
		HUD1y = y;
	}

	/** sets HUD #2 to the specified text string, color, and location */
	public void setHUD2(String string, Vector3f color, int x, int y)
	{	HUD2string = string;
		HUD2color[0]=color.x(); HUD2color[1]=color.y(); HUD2color[2]=color.z();
		HUD2x = x;
		HUD2y = y;
	}
	
	/** sets HUD #3 to the specified text string, color, and location */
	public void setHUD3(String string, Vector3f color, int x, int y)
	{	HUD3string = string;
		HUD3color[0]=color.x(); HUD3color[1]=color.y(); HUD3color[2]=color.z();
		HUD3x = x;
		HUD3y = y;
	}

	/** sets HUD #4 to the specified text string, color, and location */
	public void setHUD4(String string, Vector3f color, int x, int y) {
		HUD4string = string;
		HUD4color[0] = color.x();
		HUD4color[1] = color.y();
		HUD4color[2] = color.z();
		HUD4x = x;
		HUD4y = y;
	}

	/** sets HUD #5 to the specified text string, color, and location */
	public void setHUD5(String string, Vector3f color, int x, int y) {
		HUD5string = string;
		HUD5color[0] = color.x();
		HUD5color[1] = color.y();
		HUD5color[2] = color.z();
		HUD5x = x;
		HUD5y = y;
	}

	/** sets HUD #6 to the specified text string, color, and location */
	public void setHUD6(String string, Vector3f color, int x, int y) {
		HUD6string = string;
		HUD6color[0] = color.x();
		HUD6color[1] = color.y();
		HUD6color[2] = color.z();
		HUD6x = x;
		HUD6y = y;
	}

	/** sets HUD #7 to the specified text string, color, and location */
	public void setHUD7(String string, Vector3f color, int x, int y) {
		HUD7string = string;
		HUD7color[0] = color.x();
		HUD7color[1] = color.y();
		HUD7color[2] = color.z();
		HUD7x = x;
		HUD7y = y;
	}
	/** sets HUD #8 to the specified text string, color, and location */
	public void setHUD8(String string, Vector3f color, int x, int y) {
		HUD8string = string;
		HUD8color[0] = color.x();
		HUD8color[1] = color.y();
		HUD8color[2] = color.z();
		HUD8x = x;
		HUD8y = y;
	}
	/** sets HUD #9 to the specified text string, color, and location */
	public void setHUD9(String string, Vector3f color, int x, int y) {
		HUD9string = string;
		HUD9color[0] = color.x();
		HUD9color[1] = color.y();
		HUD9color[2] = color.z();
		HUD9x = x;
		HUD9y = y;
	}
	/** sets HUD #10 to the specified text string, color, and location */
	public void setHUD10(String string, Vector3f color, int x, int y) {
		HUD10string = string;
		HUD10color[0] = color.x();
		HUD10color[1] = color.y();
		HUD10color[2] = color.z();
		HUD10x = x;
		HUD10y = y;
	}
	/** sets HUD #11 to the specified text string, color, and location */
	public void setHUD11(String string, Vector3f color, int x, int y) {
		HUD11string = string;
		HUD11color[0] = color.x();
		HUD11color[1] = color.y();
		HUD11color[2] = color.z();
		HUD11x = x;
		HUD11y = y;
	}
	/** sets HUD #12 to the specified text string, color, and location */
	public void setHUD12(String string, Vector3f color, int x, int y) {
		HUD12string = string;
		HUD12color[0] = color.x();
		HUD12color[1] = color.y();
		HUD12color[2] = color.z();
		HUD12x = x;
		HUD12y = y;
	}
	

	/** sets HUD #1 font - available fonts are listed above. */
	public void setHUD1font(int font) { HUD1font = font; }

	/** sets HUD #2 font - available fonts are listed above. */
	public void setHUD2font(int font) { HUD2font = font; }
	
	/** sets HUD #3 font - available fonts are listed above. */
	public void setHUD3font(int font) { HUD3font = font; }

	/** sets HUD #4 font - available fonts are listed above. */
	public void setHUD4font(int font) { HUD4font = font; }

	/** sets HUD #5 font - available fonts are listed above. */
	public void setHUD5font(int font) { HUD5font = font; }

	/** sets HUD #6 font - available fonts are listed above. */
	public void setHUD6font(int font) { HUD6font = font; }

	/** sets HUD #7 font - available fonts are listed above. */
	public void setHUD7font(int font) { HUD7font = font; }

	/** sets HUD #8 font - available fonts are listed above. */
	public void setHUD8font(int font) { HUD8font = font; }
	/** sets HUD #9 font - available fonts are listed above. */
	public void setHUD9font(int font) { HUD9font = font; }
	/** sets HUD #10 font - available fonts are listed above. */
	public void setHUD10font(int font) { HUD10font = font; }
	/** sets HUD #11 font - available fonts are listed above. */
	public void setHUD11font(int font) { HUD11font = font; }	
	/** sets HUD #12 font - available fonts are listed above. */
	public void setHUD12font(int font) { HUD12font = font; }
}