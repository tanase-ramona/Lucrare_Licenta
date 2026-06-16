import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch
from matplotlib.lines import Line2D

fig, ax = plt.subplots(figsize=(22, 15))
ax.set_xlim(0, 24)
ax.set_ylim(-1, 17)
ax.axis("off")

CORE = "#cfe8ff"
LOOKUP = "#ffe9c2"
FEEDBACK = "#d8f5d0"

# name -> (x, y, w, h, [fields], color)
entities = {
    # lookup row (top)
    "Role":             (1.0,  15.0, 2.6, 0.9, ["id", "name (USER / ADMIN)"], LOOKUP),
    "Level":            (5.0,  15.0, 2.6, 0.9, ["id", "name (Junior/Mid/Senior)"], LOOKUP),
    "Position":         (9.0,  15.0, 2.8, 0.9, ["id", "name (Backend/Frontend...)"], LOOKUP),
    "Language":         (13.2, 15.0, 2.4, 0.9, ["id", "name"], LOOKUP),
    "QuestionCategory": (17.0, 15.0, 3.0, 0.9, ["id", "name (HR/TECH/PROBLEM)"], LOOKUP),

    # main flow - column 1 (user / request / interview)
    "User":             (1.0,  12.2, 3.4, 1.9, ["id", "email", "first_name, last_name", "password_hash",
                                                  "profile_level_id (FK)", "profile_position_id (FK)"], CORE),
    "InterviewRequest": (1.0,  9.3, 3.4, 1.9, ["id", "user_id (FK)", "level_id (FK)", "position_id (FK)",
                                                "languages (M:N)"], CORE),
    "Interview":        (6.0,  9.3, 3.2, 1.9, ["id", "user_id (FK)", "request_id (FK)", "status",
                                                "score", "created_at"], CORE),
    "InterviewFeedbackSummary": (10.6, 9.3, 3.8, 1.9, ["id", "interview_id (FK, 1:1)", "ready_level",
                                                         "summary_text", "strong/weak_points",
                                                         "recommended_topics", "next_steps"], FEEDBACK),

    "InterviewQuestion": (1.0, 6.4, 3.4, 1.6, ["id", "interview_id (FK)", "question_id (FK)", "order_index"], CORE),
    "InterviewAnswer":   (6.0, 6.4, 3.2, 1.6, ["id", "interview_question_id (FK)", "answer_text",
                                                "selected_option_id (FK)"], CORE),
    "AnswerFeedback":    (10.6, 6.1, 3.8, 1.9, ["id", "interview_answer_id (FK, 1:1)", "score / is_good",
                                                 "strengths / weaknesses", "improvement_tips",
                                                 "suggested_answer", "feedback_json"], FEEDBACK),

    "Question":         (1.0, 2.6, 3.6, 2.1, ["id", "category_id (FK)", "level_id (FK)", "text",
                                                "answer_type (MCQ/TEXT/CODE)", "starter_code",
                                                "languages (M:N)", "positions (M:N)"], CORE),
    "QuestionOption":   (6.2, 3.4, 3.0, 1.3, ["id", "question_id (FK)", "text", "is_correct"], CORE),
    "TestCase":         (6.2, 1.0, 3.0, 1.5, ["id", "question_id (FK)", "description",
                                                "input_data", "expected_output", "order_index"], CORE),
}

ax.text(0.4, 16.5, "Schema bazei de date — IntervYou (diagramă entitate-relație, nivel logic)",
        fontsize=16, fontweight="bold")

legend_elems = [
    FancyBboxPatch((0,0), 1, 1, fc=CORE, ec="black"),
    FancyBboxPatch((0,0), 1, 1, fc=LOOKUP, ec="black"),
    FancyBboxPatch((0,0), 1, 1, fc=FEEDBACK, ec="black"),
    Line2D([0,1],[0,0], color="black", lw=1.6),
    Line2D([0,1],[0,0], color="#a07d2a", lw=1.1, linestyle="--"),
]
ax.legend(legend_elems,
          ["Entități centrale", "Entități lookup / filtru", "Entități de feedback (AI)",
           "Relație principală (flux date)", "Relație de tip filtru / referință"],
          loc="lower left", bbox_to_anchor=(0.0, -0.06), ncol=5, frameon=False, fontsize=10)

boxes = {}
for name, (x, y, w, h, fields, color) in entities.items():
    box = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.06,rounding_size=0.08",
                         linewidth=1.3, edgecolor="black", facecolor=color, zorder=2)
    ax.add_patch(box)
    boxes[name] = (x, y, w, h)
    ax.text(x + w/2, y + h - 0.20, name, ha="center", va="top", fontsize=11.5, fontweight="bold", zorder=3)
    ax.text(x + w/2, y + h - 0.42, "\n".join(fields), ha="center", va="top",
            fontsize=7.6, zorder=3, linespacing=1.4)

def pt(name, side, frac=0.5):
    x, y, w, h = boxes[name]
    return {
        "top":    (x + w*frac, y + h),
        "bottom": (x + w*frac, y),
        "left":   (x, y + h*frac),
        "right":  (x + w, y + h*frac),
    }[side]

def poly(points, label="", color="black", lw=1.4, style="-", lpos=None):
    xs = [p[0] for p in points]
    ys = [p[1] for p in points]
    ax.plot(xs, ys, color=color, linewidth=lw, linestyle=style, zorder=1, solid_capstyle="round")
    if label:
        if lpos is None:
            mid = len(points)//2
            lpos = ((points[mid-1][0]+points[mid][0])/2, (points[mid-1][1]+points[mid][1])/2)
        ax.text(lpos[0], lpos[1], label, fontsize=8, ha="center", va="center",
                bbox=dict(boxstyle="round,pad=0.12", fc="white", ec="none"), zorder=4)

# ===== main flow (solid, orthogonal) =====
poly([pt("User","bottom",0.5), pt("InterviewRequest","top",0.5)], "1 — N", lw=1.7)
poly([pt("InterviewRequest","right"), pt("Interview","left")], "1 — N", lw=1.7)
poly([(pt("User","right")[0], pt("User","right")[1]),
      (8.0, pt("User","right")[1]), (8.0, pt("Interview","top")[1]),
      pt("Interview","top")], "1 — N", lw=1.7, lpos=(8.5, 11.6))
poly([pt("Interview","right"), pt("InterviewFeedbackSummary","left")], "1 — 1", lw=1.4)
poly([pt("Interview","bottom",0.3), pt("InterviewQuestion","top",0.85)], "1 — N", lw=1.7)
poly([pt("InterviewQuestion","right"), pt("InterviewAnswer","left")], "1 — N", lw=1.7)
poly([pt("InterviewAnswer","right"), pt("AnswerFeedback","left")], "1 — 1", lw=1.4)
poly([pt("InterviewQuestion","bottom",0.5), pt("Question","top",0.4)], "N — 1", lw=1.7)
poly([pt("Question","right",0.75), pt("QuestionOption","left")], "1 — N", lw=1.4)
poly([pt("Question","right",0.25), pt("TestCase","left")], "1 — N", lw=1.4)
poly([(pt("InterviewAnswer","bottom")[0], pt("InterviewAnswer","bottom")[1]),
      (pt("InterviewAnswer","bottom")[0], 4.75),
      (pt("QuestionOption","top")[0]+0.4, 4.75),
      pt("QuestionOption","top",0.75)],
     "N — 1\n(răspuns MCQ)", color="#555555", style="--", lw=1.0, lpos=(9.3, 5.35))

# ===== lookup / filter relations (dashed, orthogonal, routed above boxes) =====
LC = "#a07d2a"
poly([pt("User","top",0.25), (2.3, 14.55), pt("Role","bottom",0.5)], "M — N", color=LC, style="--")
poly([pt("User","top",0.6), (3.8, 14.2), pt("Level","bottom",0.15)], "N — 1 (profil)", color=LC, style="--", lpos=(4.6,13.6))
poly([pt("User","top",0.85), (4.6, 13.9), pt("Position","bottom",0.1)], "N — 1 (profil)", color=LC, style="--", lpos=(6.0,13.2))

poly([pt("InterviewRequest","left",0.85), (0.4, 10.2), (0.4, 15.45), pt("Level","left",0.3)],
     "N — 1", color=LC, style="--", lpos=(0.7, 12.6))
poly([pt("InterviewRequest","left",0.65), (-0.4, 9.9), (-0.4, 15.2), pt("Role","left",0.0), ],
     "", color=LC, style="--")
poly([pt("InterviewRequest","top",0.7), (3.6, 12.2), pt("Position","bottom",0.5)], "N — 1", color=LC, style="--", lpos=(7.3,12.0))
poly([pt("InterviewRequest","top",0.9), (4.4, 12.0), pt("Language","bottom",0.2)], "M — N", color=LC, style="--", lpos=(9.3,11.4))

poly([pt("Question","left",0.9), (0.5, 4.5), (0.5, 15.45), pt("Level","left",0.5)], "", color=LC, style="--")
poly([pt("Question","right",0.95), (5.0, 1.7), (22.6, 1.7), (22.6, 15.45), pt("Language","right",0.5)],
     "M — N", color=LC, style="--", lpos=(22.9, 8.5))
poly([pt("Question","right",0.8), (4.7, 1.4), (21.6, 1.4), (21.6, 15.2), pt("Position","right",0.3)],
     "M — N", color=LC, style="--", lpos=(21.9, 8.0))
poly([pt("Question","top",0.85), pt("QuestionCategory","left",0.0)], "N — 1", color=LC, style="--", lpos=(11.0,8.5))

plt.tight_layout()
out_path = "diagrams/IntervYou_ER_diagram.png"
plt.savefig(out_path, dpi=170, bbox_inches="tight")
print("saved:", out_path)
