-- ═══════════════════════════════════════════════════════════════════════════════
-- NipponHub Otaku Store - Data Initialization Script
-- Based on actual JPA entity models:
-- - OurUsers (ourusers table)
-- - Country (country table)  
-- - CategoriesProd (categories_prod table)
-- - Product (product table with product_country join table)
-- ═══════════════════════════════════════════════════════════════════════════════

-- Set the admin user ID for reference
SET @james_id = 1;

-- ───────────────────────────────────────────────────────────────────────────────
-- STEP 1: Verify/Create Admin User "james" in ourusers table
-- ───────────────────────────────────────────────────────────────────────────────
-- Note: If user already exists, this will be skipped due to PRIMARY KEY
INSERT IGNORE INTO ourusers (id, name, email, telephone_num, password, images, role)
VALUES (
    1,
    'James Admin',
    'james@nipponhub.com',
    '+1-555-0100',
    '$2a$10$ab0Y8SL5vkxduDywd6iuQOgwHB8rTLKuN/oXQHKF5a5pa/htw4/mS',  -- Password: 'password123' (BCrypt)
    NULL,
    'ADMIN'
);

-- ───────────────────────────────────────────────────────────────────────────────
-- STEP 2: Insert Countries into country table
-- ───────────────────────────────────────────────────────────────────────────────
INSERT INTO country (country_name, country_code, created_at) VALUES
('Japan', 'JP', NOW()),
('United States', 'US', NOW()),
('Canada', 'CA', NOW()),
('United Kingdom', 'GB', NOW()),
('France', 'FR', NOW()),
('Germany', 'DE', NOW()),
('Australia', 'AU', NOW()),
('South Korea', 'KR', NOW()),
('Singapore', 'SG', NOW()),
('Mexico', 'MX', NOW());

-- Store country IDs for later reference
SET @jp_id = (SELECT id_country FROM country WHERE country_code = 'JP');
SET @us_id = (SELECT id_country FROM country WHERE country_code = 'US');
SET @ca_id = (SELECT id_country FROM country WHERE country_code = 'CA');
SET @gb_id = (SELECT id_country FROM country WHERE country_code = 'GB');
SET @fr_id = (SELECT id_country FROM country WHERE country_code = 'FR');
SET @de_id = (SELECT id_country FROM country WHERE country_code = 'DE');
SET @au_id = (SELECT id_country FROM country WHERE country_code = 'AU');
SET @kr_id = (SELECT id_country FROM country WHERE country_code = 'KR');
SET @sg_id = (SELECT id_country FROM country WHERE country_code = 'SG');
SET @mx_id = (SELECT id_country FROM country WHERE country_code = 'MX');

-- ───────────────────────────────────────────────────────────────────────────────
-- STEP 3: Insert Product Categories into categories_prod table
-- ───────────────────────────────────────────────────────────────────────────────
INSERT INTO categories_prod (cat_prod_name, cat_prod_des) VALUES
('Manga', 'Japanese comic books and manga series'),
('Anime DVDs/Blu-ray', 'Anime series and movie releases on physical media'),
('Figures & Collectibles', 'Action figures, collectible statues, and figurines'),
('Trading Cards', 'Anime trading cards, card games, and card sets'),
('Apparel & Fashion', 'T-shirts, hoodies, jackets, and anime-themed clothing'),
('Accessories', 'Bags, keychains, straps, jewelry, and wearable items'),
('Plushies & Toys', 'Stuffed animals, plush toys, and soft collectibles'),
('Art Books & Guides', 'Artbooks, strategy guides, and illustrated collections'),
('Gaming & Merchandise', 'Video games, gaming peripherals, and gaming merchandise'),
('Cosplay & Props', 'Cosplay costumes, wigs, props, and costume accessories');

-- Store category IDs for later reference
SET @manga_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Manga');
SET @anime_dvd_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Anime DVDs/Blu-ray');
SET @figures_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Figures & Collectibles');
SET @cards_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Trading Cards');
SET @apparel_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Apparel & Fashion');
SET @accessories_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Accessories');
SET @plushies_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Plushies & Toys');
SET @artbooks_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Art Books & Guides');
SET @gaming_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Gaming & Merchandise');
SET @cosplay_id = (SELECT id_cat_prod FROM categories_prod WHERE cat_prod_name = 'Cosplay & Props');

-- ───────────────────────────────────────────────────────────────────────────────
-- STEP 4: Insert 55 Products into product table
-- ───────────────────────────────────────────────────────────────────────────────
-- Column order: prod_name, unit_price, sold_price, prod_qty, prod_description, 
--               our_user_id, created_at, id_cat_prod, franchise_id

-- ┌─ MANGA (5 products) ─────────────────────────────────────────────────────────┐
INSERT INTO product (prod_name, unit_price, sold_price, prod_qty, prod_description, ouruser_id, created_at, id_cat_prod) VALUES
('One Piece Vol 104', 8.99, 6.99, 150, 'Latest volume of the legendary adventure manga series', @james_id, NOW(), @manga_id),
('Attack on Titan Complete Collection', 45.99, 34.99, 45, 'Full box set of this acclaimed dark fantasy manga', @james_id, NOW(), @manga_id),
('My Hero Academia Vol 35', 7.99, 5.99, 200, 'Premier superhero manga - volume 35', @james_id, NOW(), @manga_id),
('Demon Slayer Manga Box Set', 89.99, 69.99, 25, 'Complete collection with exclusive slipcase and bonus material', @james_id, NOW(), @manga_id),
('Tokyo Ghoul Re: Complete Series', 79.99, 59.99, 30, 'Dark urban fantasy manga - reboot series complete collection', @james_id, NOW(), @manga_id),

-- ┌─ ANIME DVDs/BLU-RAY (6 products) ──────────────────────────────────────────┐
('Chainsaw Man Season 1 - Blu-ray', 59.99, 44.99, 80, 'Stunning 2022 action anime on premium Blu-ray with English/Japanese audio', @james_id, NOW(), @anime_dvd_id),
('Jujutsu Kaisen 0 Movie - Blu-ray', 34.99, 26.99, 120, 'Prequel movie to the acclaimed series in stunning HD', @james_id, NOW(), @anime_dvd_id),
('Cowboy Bebop Complete Series - DVD', 49.99, 37.99, 95, 'Cult classic 1998 series - all 26 episodes on DVD', @james_id, NOW(), @anime_dvd_id),
('Steins;Gate Complete Collection - Blu-ray', 79.99, 59.99, 55, 'Time travel thriller with Japanese/English audio and subtitles', @james_id, NOW(), @anime_dvd_id),
('Neon Genesis Evangelion Platinum Collection', 89.99, 69.99, 40, 'Definitive edition of the revolutionary sci-fi classic', @james_id, NOW(), @anime_dvd_id),
('Mob Psycho 100 Season 3 - Blu-ray', 49.99, 37.99, 110, 'Latest season of the supernatural comedy-drama series', @james_id, NOW(), @anime_dvd_id),

-- ┌─ FIGURES & COLLECTIBLES (12 products) ─────────────────────────────────────┐
('Sideshow Collectibles - Naruto Shippuden Statue', 249.99, 199.99, 12, 'Premium 1:4 scale polystone statue with detailed painting', @james_id, NOW(), @figures_id),
('Nendoroid Hatsune Miku', 49.99, 39.99, 85, 'Cute articulated figure from the popular Nendoroid line', @james_id, NOW(), @figures_id),
('Banpresto Ichigo Kurosaki - Bleach', 15.99, 11.99, 200, 'Claw machine prize figure - affordable collectible', @james_id, NOW(), @figures_id),
('Good Smile Company - Rem Figure', 89.99, 69.99, 45, 'Ultra-detailed 1:7 scale PVC figure from Re:Zero', @james_id, NOW(), @figures_id),
('Max Factory - Tohsaka Rin Figma', 119.99, 89.99, 30, 'Highly articulated figma with multiple accessories', @james_id, NOW(), @figures_id),
('Alter Corporation - Artoria Pendragon Statue', 159.99, 119.99, 20, 'Premium scale figure from Fate series', @james_id, NOW(), @figures_id),
('Prize Figure Lot - Assorted Anime Characters (5pcs)', 29.99, 19.99, 500, 'Bundle of 5 random prize figures - great for collectors', @james_id, NOW(), @figures_id),
('Megahouse Portrait of Pirates - Boa Hancock', 139.99, 104.99, 18, 'One Piece premium figure with detailed base', @james_id, NOW(), @figures_id),
('Turtle Hermit School - Dragon Ball Mini Figures Set', 24.99, 17.99, 120, 'Set of 5 mini figures featuring classic Dragon Ball cast', @james_id, NOW(), @figures_id),
('Godzilla vs Kong Kaiju Figure Set', 44.99, 33.99, 60, 'Articulated monster figures for display dioramas', @james_id, NOW(), @figures_id),
('Studio Trigger - Kill la Kill Ryuko Matoi Statue', 199.99, 149.99, 15, 'Highly detailed statue capturing the fierce protagonist', @james_id, NOW(), @figures_id),
('Persona 5 Joker Figma Deluxe Edition', 149.99, 109.99, 25, 'Premium articulated figure with extensive accessories', @james_id, NOW(), @figures_id),

-- ┌─ TRADING CARDS (8 products) ───────────────────────────────────────────────┐
('Pokemon TCG - Scarlet & Violet Booster Box', 129.99, 99.99, 75, '36 booster packs - official sealed product', @james_id, NOW(), @cards_id),
('Magic: The Gathering - Wilds of Eldraine Booster Box', 134.99, 104.99, 50, 'Standard MTG booster box with 36 packs', @james_id, NOW(), @cards_id),
('Yu-Gi-Oh! TCG - Burst of Destiny Booster Box', 89.99, 69.99, 100, '36 booster packs from latest Yu-Gi-Oh set', @james_id, NOW(), @cards_id),
('Digimon Card Game - Starter Deck', 14.99, 10.99, 400, 'Perfect entry point for new Digimon TCG players', @james_id, NOW(), @cards_id),
('Weiss Schwarz - Hololive Japanese Set Booster Box', 109.99, 84.99, 35, 'Anime TCG featuring VTuber characters', @james_id, NOW(), @cards_id),
('One Piece TCG - Starter Deck (Zoro)', 19.99, 14.99, 250, 'Official starter deck featuring Roronoa Zoro', @james_id, NOW(), @cards_id),
('Cardfight!! Vanguard - Booster Box D Clan Collection Vol 2', 99.99, 74.99, 40, 'Latest Vanguard booster set', @james_id, NOW(), @cards_id),
('Holographic Pokemon Card - Charizard VSTAR (PSA 8)', 599.99, 449.99, 3, 'Graded and certified rare collectible card', @james_id, NOW(), @cards_id),

-- ┌─ APPAREL & FASHION (7 products) ────────────────────────────────────────────┐
('Ghibli Studio - Spirited Away T-Shirt', 24.99, 16.99, 350, 'Classic black tee featuring No-Face artwork', @james_id, NOW(), @apparel_id),
('Dragon Ball Z - Saiyan Training Hoodie', 49.99, 34.99, 180, 'Cozy hoodie with large Saiyan emblem on back', @james_id, NOW(), @apparel_id),
('Demon Slayer Uniform Cosplay Jacket', 79.99, 59.99, 85, 'Screen-accurate replica of Tanjiro''s Demon Slayer Corps uniform', @james_id, NOW(), @apparel_id),
('Attack on Titan Scout Regiment Hoodie', 54.99, 39.99, 120, 'Official anime merchandise featuring Scout emblem', @james_id, NOW(), @apparel_id),
('Haikyuu!! Karasuno High School Jersey', 34.99, 24.99, 200, 'Official sports anime volleyball team jersey', @james_id, NOW(), @apparel_id),
('Naruto Shippuden Akatsuki Cloak Replica', 89.99, 64.99, 60, 'Premium quality full-length organization cloak', @james_id, NOW(), @apparel_id),
('Jojo''s Bizarre Adventure - Menacing T-Shirt', 22.99, 15.99, 280, 'Minimalist design capturing the show''s unique aesthetic', @james_id, NOW(), @apparel_id),

-- ┌─ ACCESSORIES (8 products) ─────────────────────────────────────────────────┐
('Death Note Notebook Replica', 17.99, 11.99, 200, 'Functional notebook designed like Light''s Death Note', @james_id, NOW(), @accessories_id),
('Genshin Impact Vision Keychain Set', 12.99, 8.99, 350, 'Set of 5 vision replicas as keychains', @james_id, NOW(), @accessories_id),
('Sailor Moon Crystal Compact Mirror', 21.99, 14.99, 150, 'Compact mirror replica with working mirror functionality', @james_id, NOW(), @accessories_id),
('Jujutsu Kaisen Sukuna Finger Pendant Necklace', 16.99, 10.99, 280, 'Resin pendant replica of Sukuna''s cursed finger', @james_id, NOW(), @accessories_id),
('Demon Slayer Tanjiro Water Sword Bottle Opener', 14.99, 9.99, 320, 'Functional bottle opener shaped like Tanjiro''s sword', @james_id, NOW(), @accessories_id),
('Studio Ghibli Totoro Plush Backpack', 34.99, 24.99, 110, 'Soft plush plushie doubling as small backpack', @james_id, NOW(), @accessories_id),
('Fire Emblem Weapons Metal Enamel Pin Set', 19.99, 12.99, 400, 'Set of 8 enamel pins featuring iconic weapons', @james_id, NOW(), @accessories_id),
('Pokemon Eevee Evolution Phone Case', 24.99, 16.99, 280, 'Durable case featuring all Eevee evolutions', @james_id, NOW(), @accessories_id),

-- ┌─ PLUSHIES & TOYS (6 products) ─────────────────────────────────────────────┐
('Pokemon Large Plush Pikachu (12")', 29.99, 19.99, 300, 'Official large size plushie with embroidered details', @james_id, NOW(), @plushies_id),
('Studio Ghibli Totoro Jumbo Plush (20")', 49.99, 34.99, 80, 'Giant size plushie - perfect for collectors', @james_id, NOW(), @plushies_id),
('My Hero Academia - All Might Mini Plush Set (5pcs)', 24.99, 16.99, 200, 'Set of 5 adorable mini plushies', @james_id, NOW(), @plushies_id),
('Sanrio Hello Kitty Plush Collection Bundle', 43.99, 29.99, 120, 'Bundle of 5 different Hello Kitty plushies', @james_id, NOW(), @plushies_id),
('Sonic the Hedgehog Squishy Toy Collection', 19.99, 12.99, 350, 'Set of 3 squishy toys featuring Sonic characters', @james_id, NOW(), @plushies_id),
('Kingdom Hearts - Heartless Plush Trio', 37.99, 24.99, 90, 'Adorable villain trio as collectible plushies', @james_id, NOW(), @plushies_id),

-- ┌─ ART BOOKS & GUIDES (5 products) ────────────────────────────────────────────┐
('Demon Slayer Official Artbook - Complete Collection', 49.99, 36.99, 75, 'High quality artbook with character designs and production artwork', @james_id, NOW(), @artbooks_id),
('Attack on Titan Comprehensive Guidebook', 44.99, 33.99, 90, 'Behind-the-scenes content and character guides', @james_id, NOW(), @artbooks_id),
('Ghibli Studio Exhibition Catalog', 64.99, 47.99, 50, 'Museum-quality exhibition catalog featuring Miyazaki''s works', @james_id, NOW(), @artbooks_id),
('Persona 5 Official Design Works', 54.99, 39.99, 60, 'Complete design document collection for the game', @james_id, NOW(), @artbooks_id),
('Legend of Zelda Breath of the Wild Artbook', 59.99, 43.99, 100, 'Environmental and character design collection', @james_id, NOW(), @artbooks_id),

-- ┌─ GAMING & MERCHANDISE (5 products) ────────────────────────────────────────┐
('Genshin Impact - Fischl Nendoroid Figure', 45.99, 33.99, 130, 'Cute articulated figure of the Electro investigator', @james_id, NOW(), @gaming_id),
('Pokemon Legends Arceus - Nintendo Switch Game', 34.99, 24.99, 200, 'Action-adventure Pokemon game for Switch', @james_id, NOW(), @gaming_id),
('Final Fantasy VII Remake - PS5 Game', 49.99, 34.99, 150, 'Modern remake of the iconic RPG', @james_id, NOW(), @gaming_id),
('Gaming Mousepad - Anime Aesthetic XXL', 29.99, 19.99, 350, 'Large mousepad featuring popular anime scenes', @james_id, NOW(), @gaming_id),
('RGB Gaming Headset - Anime Edition', 99.99, 74.99, 60, 'Premium headset with anime aesthetic design', @james_id, NOW(), @gaming_id),

-- ┌─ COSPLAY & PROPS (2 products) ─────────────────────────────────────────────┐
('Naruto Cosplay Wig Bundle - 10 Character Pack', 59.99, 39.99, 40, 'High-quality synthetic cosplay wigs for Naruto characters', @james_id, NOW(), @cosplay_id),
('Sword Art Online Kirito Katana Costume Prop', 79.99, 54.99, 30, 'Full-scale replica prop with official license', @james_id, NOW(), @cosplay_id);

-- ───────────────────────────────────────────────────────────────────────────────
-- STEP 5: Insert Many-to-Many Relationships into product_country junction table
-- ───────────────────────────────────────────────────────────────────────────────
-- Each product is assigned to 1-4 countries based on availability/distribution

-- Manga - all 10 countries
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'One Piece Vol 104'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece Vol 104'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece Vol 104'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece Vol 104'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Complete Collection'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Complete Collection'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Complete Collection'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Complete Collection'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia Vol 35'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia Vol 35'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia Vol 35'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Manga Box Set'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Manga Box Set'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Manga Box Set'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Manga Box Set'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Tokyo Ghoul Re: Complete Series'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Tokyo Ghoul Re: Complete Series'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Tokyo Ghoul Re: Complete Series'), @sg_id);

-- Anime DVDs
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Chainsaw Man Season 1 - Blu-ray'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Chainsaw Man Season 1 - Blu-ray'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Chainsaw Man Season 1 - Blu-ray'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Chainsaw Man Season 1 - Blu-ray'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Chainsaw Man Season 1 - Blu-ray'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen 0 Movie - Blu-ray'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen 0 Movie - Blu-ray'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen 0 Movie - Blu-ray'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen 0 Movie - Blu-ray'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Cowboy Bebop Complete Series - DVD'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Cowboy Bebop Complete Series - DVD'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Cowboy Bebop Complete Series - DVD'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Steins;Gate Complete Collection - Blu-ray'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Steins;Gate Complete Collection - Blu-ray'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Steins;Gate Complete Collection - Blu-ray'), @de_id),
((SELECT id_prod FROM product WHERE prod_name = 'Steins;Gate Complete Collection - Blu-ray'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Neon Genesis Evangelion Platinum Collection'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Neon Genesis Evangelion Platinum Collection'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Neon Genesis Evangelion Platinum Collection'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Mob Psycho 100 Season 3 - Blu-ray'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Mob Psycho 100 Season 3 - Blu-ray'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Mob Psycho 100 Season 3 - Blu-ray'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Mob Psycho 100 Season 3 - Blu-ray'), @kr_id);

-- Figures & Collectibles
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Sideshow Collectibles - Naruto Shippuden Statue'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sideshow Collectibles - Naruto Shippuden Statue'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sideshow Collectibles - Naruto Shippuden Statue'), @ca_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Nendoroid Hatsune Miku'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Nendoroid Hatsune Miku'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Nendoroid Hatsune Miku'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Nendoroid Hatsune Miku'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Nendoroid Hatsune Miku'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Banpresto Ichigo Kurosaki - Bleach'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Banpresto Ichigo Kurosaki - Bleach'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Banpresto Ichigo Kurosaki - Bleach'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Good Smile Company - Rem Figure'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Good Smile Company - Rem Figure'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Good Smile Company - Rem Figure'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Good Smile Company - Rem Figure'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Max Factory - Tohsaka Rin Figma'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Max Factory - Tohsaka Rin Figma'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Max Factory - Tohsaka Rin Figma'), @de_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Alter Corporation - Artoria Pendragon Statue'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Alter Corporation - Artoria Pendragon Statue'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Alter Corporation - Artoria Pendragon Statue'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Alter Corporation - Artoria Pendragon Statue'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Prize Figure Lot - Assorted Anime Characters (5pcs)'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Prize Figure Lot - Assorted Anime Characters (5pcs)'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Prize Figure Lot - Assorted Anime Characters (5pcs)'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Prize Figure Lot - Assorted Anime Characters (5pcs)'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Prize Figure Lot - Assorted Anime Characters (5pcs)'), @mx_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Megahouse Portrait of Pirates - Boa Hancock'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Megahouse Portrait of Pirates - Boa Hancock'), @us_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Turtle Hermit School - Dragon Ball Mini Figures Set'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Turtle Hermit School - Dragon Ball Mini Figures Set'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Turtle Hermit School - Dragon Ball Mini Figures Set'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Turtle Hermit School - Dragon Ball Mini Figures Set'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Godzilla vs Kong Kaiju Figure Set'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Godzilla vs Kong Kaiju Figure Set'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Godzilla vs Kong Kaiju Figure Set'), @mx_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Studio Trigger - Kill la Kill Ryuko Matoi Statue'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Trigger - Kill la Kill Ryuko Matoi Statue'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Trigger - Kill la Kill Ryuko Matoi Statue'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Joker Figma Deluxe Edition'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Joker Figma Deluxe Edition'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Joker Figma Deluxe Edition'), @au_id);

-- Trading Cards
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon TCG - Scarlet & Violet Booster Box'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon TCG - Scarlet & Violet Booster Box'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon TCG - Scarlet & Violet Booster Box'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon TCG - Scarlet & Violet Booster Box'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Magic: The Gathering - Wilds of Eldraine Booster Box'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Magic: The Gathering - Wilds of Eldraine Booster Box'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Magic: The Gathering - Wilds of Eldraine Booster Box'), @de_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Yu-Gi-Oh! TCG - Burst of Destiny Booster Box'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Yu-Gi-Oh! TCG - Burst of Destiny Booster Box'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Yu-Gi-Oh! TCG - Burst of Destiny Booster Box'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Digimon Card Game - Starter Deck'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Digimon Card Game - Starter Deck'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Digimon Card Game - Starter Deck'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Weiss Schwarz - Hololive Japanese Set Booster Box'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Weiss Schwarz - Hololive Japanese Set Booster Box'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Weiss Schwarz - Hololive Japanese Set Booster Box'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'One Piece TCG - Starter Deck (Zoro)'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece TCG - Starter Deck (Zoro)'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece TCG - Starter Deck (Zoro)'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece TCG - Starter Deck (Zoro)'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'One Piece TCG - Starter Deck (Zoro)'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Cardfight!! Vanguard - Booster Box D Clan Collection Vol 2'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Cardfight!! Vanguard - Booster Box D Clan Collection Vol 2'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Holographic Pokemon Card - Charizard VSTAR (PSA 8)'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Holographic Pokemon Card - Charizard VSTAR (PSA 8)'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Holographic Pokemon Card - Charizard VSTAR (PSA 8)'), @au_id);

-- Apparel & Fashion
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio - Spirited Away T-Shirt'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio - Spirited Away T-Shirt'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio - Spirited Away T-Shirt'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio - Spirited Away T-Shirt'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio - Spirited Away T-Shirt'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Dragon Ball Z - Saiyan Training Hoodie'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Dragon Ball Z - Saiyan Training Hoodie'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Dragon Ball Z - Saiyan Training Hoodie'), @de_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Uniform Cosplay Jacket'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Uniform Cosplay Jacket'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Uniform Cosplay Jacket'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Uniform Cosplay Jacket'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Scout Regiment Hoodie'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Scout Regiment Hoodie'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Scout Regiment Hoodie'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Haikyuu!! Karasuno High School Jersey'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Haikyuu!! Karasuno High School Jersey'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Haikyuu!! Karasuno High School Jersey'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Shippuden Akatsuki Cloak Replica'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Shippuden Akatsuki Cloak Replica'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Shippuden Akatsuki Cloak Replica'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Shippuden Akatsuki Cloak Replica'), @au_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Shippuden Akatsuki Cloak Replica'), @ca_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Jojo''s Bizarre Adventure - Menacing T-Shirt'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jojo''s Bizarre Adventure - Menacing T-Shirt'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jojo''s Bizarre Adventure - Menacing T-Shirt'), @au_id);

-- Accessories
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Death Note Notebook Replica'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Death Note Notebook Replica'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Death Note Notebook Replica'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Death Note Notebook Replica'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact Vision Keychain Set'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact Vision Keychain Set'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact Vision Keychain Set'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact Vision Keychain Set'), @sg_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact Vision Keychain Set'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Sailor Moon Crystal Compact Mirror'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sailor Moon Crystal Compact Mirror'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sailor Moon Crystal Compact Mirror'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen Sukuna Finger Pendant Necklace'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen Sukuna Finger Pendant Necklace'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Jujutsu Kaisen Sukuna Finger Pendant Necklace'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Tanjiro Water Sword Bottle Opener'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Tanjiro Water Sword Bottle Opener'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Tanjiro Water Sword Bottle Opener'), @mx_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Plush Backpack'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Plush Backpack'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Plush Backpack'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Plush Backpack'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Fire Emblem Weapons Metal Enamel Pin Set'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Fire Emblem Weapons Metal Enamel Pin Set'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Fire Emblem Weapons Metal Enamel Pin Set'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Eevee Evolution Phone Case'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Eevee Evolution Phone Case'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Eevee Evolution Phone Case'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Eevee Evolution Phone Case'), @au_id);

-- Plushies & Toys
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Large Plush Pikachu (12")'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Large Plush Pikachu (12")'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Large Plush Pikachu (12")'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Large Plush Pikachu (12")'), @au_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Large Plush Pikachu (12")'), @jp_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Jumbo Plush (20")'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Jumbo Plush (20")'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Studio Ghibli Totoro Jumbo Plush (20")'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia - All Might Mini Plush Set (5pcs)'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia - All Might Mini Plush Set (5pcs)'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia - All Might Mini Plush Set (5pcs)'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'My Hero Academia - All Might Mini Plush Set (5pcs)'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Sanrio Hello Kitty Plush Collection Bundle'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sanrio Hello Kitty Plush Collection Bundle'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sanrio Hello Kitty Plush Collection Bundle'), @au_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sanrio Hello Kitty Plush Collection Bundle'), @jp_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Sonic the Hedgehog Squishy Toy Collection'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sonic the Hedgehog Squishy Toy Collection'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sonic the Hedgehog Squishy Toy Collection'), @ca_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sonic the Hedgehog Squishy Toy Collection'), @mx_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Kingdom Hearts - Heartless Plush Trio'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Kingdom Hearts - Heartless Plush Trio'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Kingdom Hearts - Heartless Plush Trio'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Kingdom Hearts - Heartless Plush Trio'), @au_id);

-- Art Books & Guides
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Official Artbook - Complete Collection'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Official Artbook - Complete Collection'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Demon Slayer Official Artbook - Complete Collection'), @gb_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Comprehensive Guidebook'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Comprehensive Guidebook'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Comprehensive Guidebook'), @de_id),
((SELECT id_prod FROM product WHERE prod_name = 'Attack on Titan Comprehensive Guidebook'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio Exhibition Catalog'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio Exhibition Catalog'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio Exhibition Catalog'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Ghibli Studio Exhibition Catalog'), @fr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Official Design Works'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Official Design Works'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Persona 5 Official Design Works'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Legend of Zelda Breath of the Wild Artbook'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Legend of Zelda Breath of the Wild Artbook'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Legend of Zelda Breath of the Wild Artbook'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Legend of Zelda Breath of the Wild Artbook'), @ca_id);

-- Gaming & Merchandise
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact - Fischl Nendoroid Figure'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact - Fischl Nendoroid Figure'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact - Fischl Nendoroid Figure'), @kr_id),
((SELECT id_prod FROM product WHERE prod_name = 'Genshin Impact - Fischl Nendoroid Figure'), @sg_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Legends Arceus - Nintendo Switch Game'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Legends Arceus - Nintendo Switch Game'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Legends Arceus - Nintendo Switch Game'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Pokemon Legends Arceus - Nintendo Switch Game'), @au_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Final Fantasy VII Remake - PS5 Game'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Final Fantasy VII Remake - PS5 Game'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Final Fantasy VII Remake - PS5 Game'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Final Fantasy VII Remake - PS5 Game'), @de_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Gaming Mousepad - Anime Aesthetic XXL'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Gaming Mousepad - Anime Aesthetic XXL'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Gaming Mousepad - Anime Aesthetic XXL'), @de_id),
((SELECT id_prod FROM product WHERE prod_name = 'Gaming Mousepad - Anime Aesthetic XXL'), @kr_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'RGB Gaming Headset - Anime Edition'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'RGB Gaming Headset - Anime Edition'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'RGB Gaming Headset - Anime Edition'), @de_id);

-- Cosplay & Props
INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Cosplay Wig Bundle - 10 Character Pack'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Cosplay Wig Bundle - 10 Character Pack'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Cosplay Wig Bundle - 10 Character Pack'), @jp_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Cosplay Wig Bundle - 10 Character Pack'), @au_id),
((SELECT id_prod FROM product WHERE prod_name = 'Naruto Cosplay Wig Bundle - 10 Character Pack'), @ca_id);

INSERT INTO product_country (product_id, country_id) VALUES
((SELECT id_prod FROM product WHERE prod_name = 'Sword Art Online Kirito Katana Costume Prop'), @us_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sword Art Online Kirito Katana Costume Prop'), @gb_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sword Art Online Kirito Katana Costume Prop'), @de_id),
((SELECT id_prod FROM product WHERE prod_name = 'Sword Art Online Kirito Katana Costume Prop'), @au_id);

-- ───────────────────────────────────────────────────────────────────────────────
-- VERIFICATION QUERIES
-- ───────────────────────────────────────────────────────────────────────────────

-- Verify user was created
SELECT 'ADMIN USER' as Type, userid, name, email, role FROM ourusers WHERE userid = @james_id;

-- Verify countries
SELECT 'COUNTRIES' as Type, COUNT(*) as Count FROM country;
SELECT country_name, country_code FROM country ORDER BY country_name;

-- Verify categories
SELECT 'CATEGORIES' as Type, COUNT(*) as Count FROM categories_prod;
SELECT cat_prod_name, COUNT(p.id_prod) as ProductCount 
FROM categories_prod cp 
LEFT JOIN product p ON cp.id_cat_prod = p.id_cat_prod
GROUP BY cp.idCatProd, cp.cat_prod_name
ORDER BY cp.cat_prod_name;

-- Verify products
SELECT 'PRODUCTS' as Type, COUNT(*) as Count FROM product;
SELECT 
    p.prod_name,
    cp.cat_prod_name as Category,
    p.prod_qty as Stock,
    p.unit_price,
    p.sold_price,
    CONCAT('$', FORMAT(p.sold_price, 2)) as Retail,
    COUNT(DISTINCT pc.country_id) as AvailableInCountries
FROM product p
LEFT JOIN categories_prod cp ON p.id_cat_prod = cp.id_cat_prod
LEFT JOIN product_country pc ON p.id_prod = pc.product_id
GROUP BY p.id_prod, p.prod_name, cp.cat_prod_name, p.prod_qty, p.unit_price, p.sold_price
ORDER BY cp.cat_prod_name, p.prod_name;

-- Verify many-to-many relationships
SELECT 'PRODUCT-COUNTRY MAPPINGS' as Type, COUNT(*) as Count FROM product_country;

-- Show sample product availability
SELECT 
    p.prod_name,
    GROUP_CONCAT(c.country_name ORDER BY c.country_name SEPARATOR ', ') as AvailableIn
FROM product p
LEFT JOIN product_country pc ON p.id_prod = pc.product_id
LEFT JOIN country c ON pc.country_id = c.id_country
WHERE p.prod_name IN ('One Piece Vol 104', 'Chainsaw Man Season 1 - Blu-ray', 'Pokemon TCG - Scarlet & Violet Booster Box')
GROUP BY p.id_prod, p.prod_name;

-- Count inventory value
SELECT 
    CONCAT('$', FORMAT(SUM(p.sold_price * p.prod_qty), 2)) as TotalInventoryValue,
    SUM(p.prod_qty) as TotalUnitsInStock
FROM product p;
