//package com.company.bank_system.security;
//
//import com.company.bank_system.repo.CardRepository;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//
//@Component("cardPermission")
//public class CardPermission {
//
//    private final CardRepository cardRepository;
//
//    public CardPermission(CardRepository cardRepository) {
//        this.cardRepository = cardRepository;
//    }
//
//    public boolean canBlock(Long cardId, Authentication authentication) {
//        System.out.println("=== CardPermission Debug ===");
//        System.out.println("Card ID: " + cardId);
//        System.out.println("Authentication: " + authentication);
//        System.out.println("Principal: " + authentication.getPrincipal());
//        System.out.println("Authorities: " + authentication.getAuthorities());
//
//        Long userId = (Long) authentication.getPrincipal();
//        System.out.println("User ID from auth: " + userId);
//
//        boolean exists = cardRepository.existsById(cardId);
//        System.out.println("Card exists: " + exists);
//
//        boolean isOwner = cardRepository.existsByIdAndUserId(cardId, userId);
//        System.out.println("Is owner: " + isOwner);
//
//        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
//        System.out.println("Is admin: " + isAdmin);
//        System.out.println("=== End Debug ===");
//
//        return isOwner || isAdmin;
//    }
//
//}
