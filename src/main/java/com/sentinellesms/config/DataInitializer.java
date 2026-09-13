package com.sentinellesms.config;

import com.sentinellesms.entity.ContentItem;
import com.sentinellesms.entity.ContentType;
import com.sentinellesms.entity.FraudPattern;
import com.sentinellesms.entity.Role;
import com.sentinellesms.entity.User;
import com.sentinellesms.repository.ContentItemRepository;
import com.sentinellesms.repository.FraudPatternRepository;
import com.sentinellesms.repository.RoleRepository;
import com.sentinellesms.repository.UserRepository;
import com.sentinellesms.security.Roles;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContentItemRepository contentItemRepository;
    private final FraudPatternRepository fraudPatternRepository;

    @Value("${sentinellesms.admin.username}")
    private String adminUsername;

    @Value("${sentinellesms.admin.email}")
    private String adminEmail;

    @Value("${sentinellesms.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Role userRole = ensureRole(Roles.USER);
        ensureRole(Roles.ADMIN);
        ensureRole(Roles.MODERATOR);
        ensureRole(Roles.ANALYST);
        Role superAdminRole = ensureRole(Roles.SUPER_ADMIN);
        ensureAdminUser(userRole, superAdminRole);
        seedDefaultContent();
        seedDefaultPatterns();
    }

    private Role ensureRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(null, name)));
    }

    private void ensureAdminUser(Role userRole, Role superAdminRole) {
        User existing = userRepository.findByUsername(adminUsername)
                .or(() -> userRepository.findByEmail(adminEmail))
                .orElse(null);

        if (existing != null) {
            if (existing.getRoles().stream().noneMatch(r -> Roles.SUPER_ADMIN.equals(r.getName()))) {
                existing.getRoles().add(superAdminRole);
                userRepository.save(existing);
            }
            return;
        }

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(superAdminRole);

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setRoles(roles);

        userRepository.save(admin);
    }

    private void seedDefaultContent() {
        seedContent("alert.high_risk", ContentType.ALERT, 1,
                "Alerte risque élevé",
                "Attention : ce message ou ce lien présente un risque important. "
                        + "Ne communiquez jamais votre PIN, OTP, mot de passe ou données sensibles.");
        seedContent("alert.critical", ContentType.ALERT, 2,
                "Alerte critique",
                "Danger critique détecté. N'ouvrez pas le lien, ne répondez pas et ne payez rien. "
                        + "Signalez la fraude si possible.");
        seedContent("explain.urgence", ContentType.EXPLANATION, 10,
                "Urgence artificielle",
                "Les fraudeurs créent un sentiment d'urgence pour vous pousser à agir sans réfléchir.");
        seedContent("explain.pin_otp", ContentType.EXPLANATION, 11,
                "Demande de PIN ou OTP",
                "Aucun agent légitime ne vous demandera votre code secret Mobile Money ou un code OTP par SMS.");
        seedContent("explain.lien", ContentType.EXPLANATION, 12,
                "Lien suspect",
                "Un lien peut imiter Orange Money, Moov, Malitel ou Wave pour voler vos identifiants.");
        seedContent("privacy.cni", ContentType.PRIVACY_TIP, 20,
                "Pièce d'identité",
                "Ne partagez jamais de photo de votre CNI, passeport ou documents personnels via SMS ou WhatsApp.");
        seedContent("privacy.codes", ContentType.PRIVACY_TIP, 21,
                "Codes et mots de passe",
                "PIN, OTP et mots de passe sont personnels. Sentinelle Mali ne vous les demandera jamais.");
        seedContent("victim.step1", ContentType.VICTIM_GUIDE, 30,
                "1. Sécurisez vos comptes",
                "Changez immédiatement vos codes Mobile Money et mots de passe. Contactez votre opérateur pour bloquer "
                        + "toute transaction suspecte.");
        seedContent("victim.step2", ContentType.VICTIM_GUIDE, 31,
                "2. Conservez les preuves",
                "Gardez les SMS, captures d'écran, numéros et liens reçus. Ne supprimez rien avant d'avoir signalé.");
        seedContent("victim.step3", ContentType.VICTIM_GUIDE, 32,
                "3. Signalez la fraude",
                "Prévenez votre opérateur, déposez plainte auprès de la police ou de la gendarmerie, "
                        + "et signalez aussi dans Sentinelle Mali.");
    }

    private void seedContent(String key, ContentType type, int order, String title, String body) {
        if (contentItemRepository.existsByKeyAndLanguage(key, "fr")) {
            return;
        }
        ContentItem item = new ContentItem();
        item.setKey(key);
        item.setType(type);
        item.setLanguage("fr");
        item.setTitle(title);
        item.setBody(body);
        item.setSortOrder(order);
        item.setActive(true);
        contentItemRepository.save(item);
    }

    private void seedDefaultPatterns() {
        if (fraudPatternRepository.count() > 0) {
            return;
        }
        List<FraudPattern> patterns = List.of(
                pattern("Faux agent Mobile Money", "agent orange,agent moov,agent malitel,agent wave",
                        "MOBILE_MONEY", "HIGH", 75, "agent,urgence",
                        "Usurpation d'un agent Mobile Money demandant un code ou une validation."),
                pattern("Demande de PIN/OTP", "envoyez votre code,donnez votre pin,code otp,code secret",
                        "MOBILE_MONEY", "CRITICAL", 90, "pin,otp",
                        "Demande explicite de code secret ou OTP."),
                pattern("Faux gain / concours", "vous avez gagné,félicitations,concours,recevez 500000",
                        "SCAM", "HIGH", 70, "gain,urgence",
                        "Fausse promesse de gain pour soutirer de l'argent ou des codes."),
                pattern("Faux remboursement", "remboursement,trop perçu,erreur de transfert,argent à récupérer",
                        "MOBILE_MONEY", "HIGH", 72, "remboursement,argent",
                        "Scénario de faux remboursement ou erreur de transfert."),
                pattern("Faux blocage de compte", "compte bloqué,compte suspendu,régularisez immédiatement",
                        "MOBILE_MONEY", "HIGH", 68, "blocage,urgence",
                        "Menace de blocage de compte pour forcer une action.")
        );
        fraudPatternRepository.saveAll(patterns);
    }

    private FraudPattern pattern(String label, String keywords, String category, String riskLevel,
                                 int riskScore, String signalCodes, String description) {
        FraudPattern p = new FraudPattern();
        p.setLabel(label);
        p.setKeywords(keywords);
        p.setLanguage("fr");
        p.setCategory(category);
        p.setRiskLevel(riskLevel);
        p.setRiskScore(riskScore);
        p.setSignalCodes(signalCodes);
        p.setDescription(description);
        p.setActive(true);
        return p;
    }
}
