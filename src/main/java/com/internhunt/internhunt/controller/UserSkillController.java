package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.Skill;
import com.internhunt.internhunt.entity.User;
import com.internhunt.internhunt.entity.UserSkill;
import com.internhunt.internhunt.repository.SkillRepository;
import com.internhunt.internhunt.repository.UserRepository;
import com.internhunt.internhunt.repository.UserSkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages skills on a user's profile.
 *
 * GET    /api/users/{userId}/skills           — list user's skills
 * POST   /api/users/{userId}/skills           — add a skill { skillId, proficiency }
 * PUT    /api/users/{userId}/skills/{skillId} — update proficiency
 * DELETE /api/users/{userId}/skills/{skillId} — remove a skill
 */
@RestController
@RequestMapping("/api/users/{userId}/skills")
public class UserSkillController
{
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private UserRepository      userRepository;
    @Autowired private SkillRepository     skillRepository;

    @GetMapping
    public ResponseEntity<?> getUserSkills(@PathVariable Integer userId)
    {
        if (userRepository.findById(userId).isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(userSkillRepository.findByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> addSkill(
            @PathVariable Integer userId,
            @RequestBody Map<String, Object> body)
    {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        Integer skillId = (Integer) body.get("skillId");
        Integer proficiency = body.containsKey("proficiency")
                ? (Integer) body.get("proficiency") : 1;

        if (skillId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "skillId is required"));
        if (proficiency < 1 || proficiency > 3)
            return ResponseEntity.badRequest().body(Map.of("error", "proficiency must be 1, 2, or 3"));

        Optional<Skill> skillOpt = skillRepository.findById(skillId);
        if (skillOpt.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Skill not found"));

        // Check for duplicate
        List<UserSkill> existing = userSkillRepository.findByUserId(userId);
        boolean alreadyHas = existing.stream().anyMatch(us -> us.getSkill().getId().equals(skillId));
        if (alreadyHas)
            return ResponseEntity.badRequest().body(Map.of("error", "User already has this skill"));

        UserSkill userSkill = new UserSkill();
        userSkill.setUser(userOpt.get());
        userSkill.setSkill(skillOpt.get());
        userSkill.setProficiency(proficiency);

        return ResponseEntity.ok(userSkillRepository.save(userSkill));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<?> updateSkillProficiency(
            @PathVariable Integer userId,
            @PathVariable Integer skillId,
            @RequestBody Map<String, Object> body)
    {
        Integer proficiency = (Integer) body.get("proficiency");
        if (proficiency == null || proficiency < 1 || proficiency > 3)
            return ResponseEntity.badRequest().body(Map.of("error", "proficiency must be 1, 2, or 3"));

        List<UserSkill> skills = userSkillRepository.findByUserId(userId);
        Optional<UserSkill> match = skills.stream()
                .filter(us -> us.getSkill().getId().equals(skillId))
                .findFirst();

        if (match.isEmpty()) return ResponseEntity.notFound().build();

        UserSkill us = match.get();
        us.setProficiency(proficiency);
        return ResponseEntity.ok(userSkillRepository.save(us));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<?> removeSkill(
            @PathVariable Integer userId,
            @PathVariable Integer skillId)
    {
        List<UserSkill> skills = userSkillRepository.findByUserId(userId);
        Optional<UserSkill> match = skills.stream()
                .filter(us -> us.getSkill().getId().equals(skillId))
                .findFirst();

        if (match.isEmpty()) return ResponseEntity.notFound().build();

        userSkillRepository.deleteById(match.get().getId());
        return ResponseEntity.noContent().build();
    }
}

