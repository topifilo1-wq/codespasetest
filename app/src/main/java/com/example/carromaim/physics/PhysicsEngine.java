package com.example.carromaim.physics;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

public class PhysicsEngine {

    public static class TrajectoryLine {
        public Vector2D start;
        public Vector2D end;
        public int lineType; // 0 = Direct, 1 = Target1, 2 = Target2 (Chain), 3 = Deflection, 4 = Wall Bounce

        public TrajectoryLine(Vector2D start, Vector2D end, int lineType) {
            this.start = start;
            this.end = end;
            this.lineType = lineType;
        }
    }

    public static class GhostBall {
        public Vector2D position;
        public float radius;
        public int color;

        public GhostBall(Vector2D position, float radius, int color) {
            this.position = position;
            this.radius = radius;
            this.color = color;
        }
    }

    public static class SimulationResult {
        public List<TrajectoryLine> lines = new ArrayList<>();
        public List<GhostBall> ghostBalls = new ArrayList<>();
        public boolean pocketTargetDetected = false;
    }

    public static class PieceCollision {
        public boolean hit = false;
        public CarromPiece targetPiece;
        public Vector2D impactPos;
        public Vector2D targetDirection;
        public Vector2D deflectionDirection;
        public float distance = Float.MAX_VALUE;
    }

    public static class WallCollision {
        public boolean hit = false;
        public Vector2D impactPos;
        public Vector2D reflectedDir;
        public float distance = Float.MAX_VALUE;
    }

    public static PieceCollision checkPieceCollision(CarromPiece movingPiece, Vector2D dir, List<CarromPiece> pieces) {
        PieceCollision result = new PieceCollision();
        Vector2D d = dir.normalize();

        for (CarromPiece piece : pieces) {
            if (piece.id == movingPiece.id) continue;

            Vector2D f = movingPiece.position.sub(piece.position);
            float combinedRadius = movingPiece.radius + piece.radius;

            float a = d.dot(d);
            float b = 2.0f * f.dot(d);
            float c = f.dot(f) - (combinedRadius * combinedRadius);

            float discriminant = b * b - 4 * a * c;
            if (discriminant >= 0) {
                float sqrtDisc = (float) Math.sqrt(discriminant);
                float t = (-b - sqrtDisc) / (2.0f * a);

                if (t > 0.01f && t < result.distance) {
                    result.distance = t;
                    result.hit = true;
                    result.targetPiece = piece;
                    result.impactPos = movingPiece.position.add(d.scale(t));

                    Vector2D normal = piece.position.sub(result.impactPos).normalize();
                    result.targetDirection = normal;

                    float normalVel = d.dot(normal);
                    result.deflectionDirection = d.sub(normal.scale(normalVel)).normalize();
                }
            }
        }
        return result;
    }

    public static WallCollision checkWallCollision(Vector2D start, Vector2D dir, float radius, RectF bounds) {
        WallCollision result = new WallCollision();
        Vector2D d = dir.normalize();

        float minX = bounds.left + radius;
        float maxX = bounds.right - radius;
        float minY = bounds.top + radius;
        float maxY = bounds.bottom - radius;

        if (d.x > 0.0001f) {
            float t = (maxX - start.x) / d.x;
            if (t > 0 && t < result.distance) {
                float y = start.y + d.y * t;
                if (y >= minY && y <= maxY) {
                    result.distance = t;
                    result.hit = true;
                    result.impactPos = new Vector2D(maxX, y);
                    result.reflectedDir = new Vector2D(-d.x, d.y);
                }
            }
        } else if (d.x < -0.0001f) {
            float t = (minX - start.x) / d.x;
            if (t > 0 && t < result.distance) {
                float y = start.y + d.y * t;
                if (y >= minY && y <= maxY) {
                    result.distance = t;
                    result.hit = true;
                    result.impactPos = new Vector2D(minX, y);
                    result.reflectedDir = new Vector2D(-d.x, d.y);
                }
            }
        }

        if (d.y > 0.0001f) {
            float t = (maxY - start.y) / d.y;
            if (t > 0 && t < result.distance) {
                float x = start.x + d.x * t;
                if (x >= minX && x <= maxX) {
                    result.distance = t;
                    result.hit = true;
                    result.impactPos = new Vector2D(x, maxY);
                    result.reflectedDir = new Vector2D(d.x, -d.y);
                }
            }
        } else if (d.y < -0.0001f) {
            float t = (minY - start.y) / d.y;
            if (t > 0 && t < result.distance) {
                float x = start.x + d.x * t;
                if (x >= minX && x <= maxX) {
                    result.distance = t;
                    result.hit = true;
                    result.impactPos = new Vector2D(x, minY);
                    result.reflectedDir = new Vector2D(d.x, -d.y);
                }
            }
        }

        return result;
    }

    public static SimulationResult simulate(CarromPiece striker, Vector2D aimDir, List<CarromPiece> pieces, RectF bounds, List<Vector2D> pockets, float pocketRadius) {
        SimulationResult result = new SimulationResult();
        Vector2D currentDir = aimDir.normalize();
        Vector2D currentStart = striker.position;

        PieceCollision hit1 = checkPieceCollision(striker, currentDir, pieces);
        WallCollision wall1 = checkWallCollision(currentStart, currentDir, striker.radius, bounds);

        if (hit1.hit && hit1.distance < wall1.distance) {
            // Hits Piece 1 directly
            result.lines.add(new TrajectoryLine(currentStart, hit1.impactPos, 0));
            result.ghostBalls.add(new GhostBall(hit1.impactPos, striker.radius, 0x88FFFFFF));

            // Deflection of striker
            Vector2D defEnd = hit1.impactPos.add(hit1.deflectionDirection.scale(120f));
            result.lines.add(new TrajectoryLine(hit1.impactPos, defEnd, 3));

            // Direction of target piece 1
            CarromPiece p1 = hit1.targetPiece;
            CarromPiece movingP1 = new CarromPiece(p1.id, p1.position.x, p1.position.y, p1.radius, p1.color, false);
            
            // Chain collision check: does target piece 1 hit piece 2?
            PieceCollision hit2 = checkPieceCollision(movingP1, hit1.targetDirection, pieces);
            WallCollision p1Wall = checkWallCollision(movingP1.position, hit1.targetDirection, p1.radius, bounds);

            if (hit2.hit && hit2.distance < p1Wall.distance) {
                // Piece 1 hits Piece 2 (Combo / Chain shot)
                result.lines.add(new TrajectoryLine(p1.position, hit2.impactPos, 1));
                result.ghostBalls.add(new GhostBall(hit2.impactPos, p1.radius, 0x88FFFF00));

                // Piece 2 trajectory
                Vector2D p2End = hit2.targetPiece.position.add(hit2.targetDirection.scale(250f));
                result.lines.add(new TrajectoryLine(hit2.targetPiece.position, p2End, 2));

                // Check pocket for piece 2
                checkPocketTarget(hit2.targetPiece.position, hit2.targetDirection, pockets, pocketRadius, result);
            } else {
                // Piece 1 moves towards wall or pocket
                Vector2D end1 = p1Wall.hit ? p1Wall.impactPos : p1.position.add(hit1.targetDirection.scale(300f));
                result.lines.add(new TrajectoryLine(p1.position, end1, 1));
                checkPocketTarget(p1.position, hit1.targetDirection, pockets, pocketRadius, result);
            }

        } else if (wall1.hit) {
            // Striker hits wall first (Cushion Rebound shot)
            result.lines.add(new TrajectoryLine(currentStart, wall1.impactPos, 0));
            
            // Rebound piece
            CarromPiece reboundPiece = new CarromPiece(striker.id, wall1.impactPos.x, wall1.impactPos.y, striker.radius, striker.color, true);
            PieceCollision reboundHit = checkPieceCollision(reboundPiece, wall1.reflectedDir, pieces);
            
            if (reboundHit.hit) {
                result.lines.add(new TrajectoryLine(wall1.impactPos, reboundHit.impactPos, 4));
                result.ghostBalls.add(new GhostBall(reboundHit.impactPos, striker.radius, 0x88FFFFFF));
                
                Vector2D reboundTargetEnd = reboundHit.targetPiece.position.add(reboundHit.targetDirection.scale(250f));
                result.lines.add(new TrajectoryLine(reboundHit.targetPiece.position, reboundTargetEnd, 1));
                checkPocketTarget(reboundHit.targetPiece.position, reboundHit.targetDirection, pockets, pocketRadius, result);
            } else {
                Vector2D wallReboundEnd = wall1.impactPos.add(wall1.reflectedDir.scale(300f));
                result.lines.add(new TrajectoryLine(wall1.impactPos, wallReboundEnd, 4));
            }
        }

        return result;
    }

    private static void checkPocketTarget(Vector2D start, Vector2D dir, List<Vector2D> pockets, float pocketRadius, SimulationResult result) {
        Vector2D d = dir.normalize();
        for (Vector2D pocket : pockets) {
            Vector2D toPocket = pocket.sub(start);
            float proj = toPocket.dot(d);
            if (proj > 0) {
                Vector2D closestPoint = start.add(d.scale(proj));
                if (closestPoint.distanceTo(pocket) < pocketRadius * 1.5f) {
                    result.pocketTargetDetected = true;
                    break;
                }
            }
        }
    }
}
