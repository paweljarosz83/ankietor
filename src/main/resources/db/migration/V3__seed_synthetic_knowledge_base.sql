-- DANE SYNTETYCZNE. Domena odzwierciedla realny obszar ankiet klienckich
-- (certyfikaty, systemy zarzadzania, ubezpieczenia, zgodnosc, dane rejestrowe),
-- ale kazda tresc jest wymyslona. Numery certyfikatow, polis, KRS, NIP i nazwy
-- jednostek nie odnosza sie do zadnego istniejacego podmiotu.
--
-- Zestaw jest wsadem dla dopasowania leksykalnego i punktem odniesienia dla testow
-- kryteriow A1-A3 oraz B1 z context/foundation/prd.md.
--
-- author_id pozostaje NULL - konto administratora powstaje po migracjach.

INSERT INTO question_answers (question, answer) VALUES

-- === Systemy zarzadzania jakoscia ===
('Czy posiadają Państwo aktualny certyfikat ISO 9001?',
 'Tak. Spółka posiada certyfikat systemu zarządzania jakością ISO 9001:2015 nr QMS-2019-4471, wydany przez akredytowaną jednostkę certyfikującą, ważny do 31.03.2027. Kopia certyfikatu stanowi załącznik do ankiety.'),

('Prosimy o podanie numeru i daty ważności certyfikatu ISO 9001.',
 'Certyfikat ISO 9001:2015 nr QMS-2019-4471, data wydania 01.04.2019, ostatni recertyfikat 15.03.2024, data ważności 31.03.2027.'),

('Kto jest jednostką certyfikującą Państwa system zarządzania jakością?',
 'Certyfikacja prowadzona jest przez jednostkę akredytowaną przy krajowym centrum akredytacji, numer akredytacji AC-118. Audyty nadzoru odbywają się raz w roku.'),

('Jak często przeprowadzane są audyty wewnętrzne systemu jakości?',
 'Audyty wewnętrzne prowadzone są zgodnie z rocznym harmonogramem, obejmują wszystkie procesy objęte systemem i realizowane są nie rzadziej niż raz w roku dla każdego procesu.'),

('Czy w ostatnich trzech latach zawieszono lub wycofano Państwu jakikolwiek certyfikat?',
 'Nie. W okresie ostatnich trzech lat nie doszło do zawieszenia ani wycofania żadnego z posiadanych certyfikatów. Wszystkie audyty nadzoru zakończyły się wynikiem pozytywnym.'),

-- === Srodowisko ===
('Czy posiadają Państwo certyfikat ISO 14001?',
 'Tak. System zarządzania środowiskowego certyfikowany zgodnie z ISO 14001:2015, certyfikat nr EMS-2020-1180, ważny do 30.09.2026.'),

('Proszę opisać Państwa politykę środowiskową.',
 'Polityka środowiskowa obejmuje ograniczanie zużycia energii i wody, segregację i odzysk odpadów produkcyjnych, monitoring emisji oraz roczne cele redukcyjne zatwierdzane przez zarząd. Dokument jest publicznie dostępny.'),

('Jak Państwo postępują z odpadami produkcyjnymi?',
 'Odpady są segregowane u źródła i przekazywane wyłącznie podmiotom posiadającym wymagane zezwolenia. Prowadzona jest pełna ewidencja odpadów w systemie krajowym.'),

('Czy prowadzą Państwo ewidencję śladu węglowego?',
 'Tak, dla zakresów 1 i 2 od roku obrachunkowego 2023. Zakres 3 jest w trakcie wdrażania, planowane pierwsze raportowanie za rok 2026.'),

-- === BHP ===
('Czy posiadają Państwo certyfikat ISO 45001?',
 'Tak. System zarządzania bezpieczeństwem i higieną pracy certyfikowany zgodnie z ISO 45001:2018, certyfikat nr OHS-2021-0663, ważny do 14.05.2027.'),

('Jaki był wskaźnik wypadków przy pracy w ostatnim roku?',
 'W ostatnim pełnym roku obrachunkowym odnotowano 3 wypadki lekkie, brak wypadków ciężkich i zbiorowych. Wskaźnik częstotliwości wyniósł 4,1 na milion godzin pracy.'),

('Czy pracownicy przechodzą szkolenia BHP przed dopuszczeniem do pracy?',
 'Tak. Każdy pracownik przechodzi szkolenie wstępne przed dopuszczeniem do pracy oraz szkolenia okresowe w terminach wynikających z przepisów i stanowiska.'),

-- === Ubezpieczenia ===
('Czy posiadają Państwo ubezpieczenie od odpowiedzialności cywilnej?',
 'Tak. Ubezpieczenie odpowiedzialności cywilnej z tytułu prowadzonej działalności i posiadanego mienia, polisa nr OC-4471-2026, suma gwarancyjna 20 mln PLN na jedno i wszystkie zdarzenia, ważna do 31.12.2026.'),

('Proszę podać numer polisy OC oraz sumę gwarancyjną.',
 'Polisa nr OC-4471-2026, suma gwarancyjna 20 mln PLN na jedno i wszystkie zdarzenia w okresie ubezpieczenia.'),

('Czy towar w transporcie jest objęty ubezpieczeniem?',
 'Tak. Ładunki w transporcie objęte są umową ubezpieczenia generalnego cargo, polisa nr CARGO-2026-0912, zakres obejmuje transport krajowy i międzynarodowy.'),

('Czy posiadają Państwo ubezpieczenie od utraty zysku?',
 'Tak, w ramach polisy majątkowej rozszerzonej o klauzulę utraty zysku, z okresem odszkodowawczym 12 miesięcy.'),

-- === Dane rejestrowe i podatkowe ===
('Proszę podać numer KRS oraz NIP spółki.',
 'KRS 0000118844, NIP 6771182204, REGON 351884120. Sąd rejestrowy: Sąd Rejonowy właściwy dla siedziby spółki, Wydział Gospodarczy Krajowego Rejestru Sądowego.'),

('Jaka jest forma prawna i wysokość kapitału zakładowego?',
 'Spółka akcyjna. Kapitał zakładowy 48 000 000 PLN, wpłacony w całości.'),

('Czy są Państwo czynnym podatnikiem VAT?',
 'Tak. Spółka jest zarejestrowana jako czynny podatnik VAT oraz posiada aktywny numer VAT-UE. Zaświadczenie o rejestracji dostępne na życzenie.'),

('Czy mogą Państwo przedstawić certyfikat rezydencji podatkowej?',
 'Tak. Aktualny certyfikat rezydencji podatkowej wydawany jest przez właściwy urząd skarbowy na wniosek i przekazywany kontrahentowi w terminie do 10 dni roboczych.'),

('Czy posiadają Państwo zaświadczenie o niezaleganiu z podatkami?',
 'Tak. Zaświadczenia o niezaleganiu w podatkach oraz w opłacaniu składek na ubezpieczenia społeczne wydawane są na wniosek i przekazywane w terminie do 14 dni.'),

('Kto jest uprawniony do reprezentowania spółki?',
 'Reprezentacja zgodna z wpisem w rejestrze przedsiębiorców. Do składania oświadczeń woli uprawnieni są dwaj członkowie zarządu łącznie albo członek zarządu z prokurentem.'),

-- === Poufnosc i dane osobowe ===
('Czy są Państwo gotowi zawrzeć umowę o zachowaniu poufności?',
 'Tak. Spółka zawiera umowy o zachowaniu poufności na wzorze własnym lub kontrahenta, po weryfikacji przez dział prawny. Standardowy czas weryfikacji to 5 dni roboczych.'),

('Jak długo obowiązuje zobowiązanie do zachowania poufności po zakończeniu współpracy?',
 'Standardowy okres obowiązywania zobowiązania to 5 lat od zakończenia współpracy, z zastrzeżeniem informacji stanowiących tajemnicę przedsiębiorstwa, dla których obowiązuje bezterminowo.'),

('Kto jest u Państwa administratorem danych osobowych?',
 'Administratorem danych osobowych jest spółka. Wyznaczony został inspektor ochrony danych, kontakt przez adres wskazany w polityce prywatności.'),

('Czy przekazują Państwo dane osobowe poza Europejski Obszar Gospodarczy?',
 'Nie w ramach standardowej współpracy handlowej. Ewentualne przekazanie odbywa się wyłącznie na podstawie standardowych klauzul umownych i po ocenie skutków transferu.'),

('Czy w ciągu ostatnich dwóch lat wystąpiło u Państwa naruszenie ochrony danych osobowych?',
 'Nie odnotowano naruszeń podlegających zgłoszeniu do organu nadzorczego. Procedura zgłaszania naruszeń jest wdrożona i testowana raz w roku.'),

-- === Zgodnosc produktowa ===
('Czy Państwa wyroby spełniają wymagania dyrektywy RoHS?',
 'Tak. Wyroby objęte zakresem dyrektywy spełniają ograniczenia zawartości substancji niebezpiecznych. Deklaracje zgodności wydawane są dla każdej rodziny wyrobów.'),

('Czy substancje w Państwa wyrobach są zarejestrowane zgodnie z REACH?',
 'Tak. Substancje wchodzące w skład wyrobów są zarejestrowane zgodnie z wymaganiami rozporządzenia REACH. Prowadzony jest monitoring listy kandydackiej SVHC.'),

('Czy dostarczają Państwo deklaracje zgodności do wyrobów?',
 'Tak. Do każdej dostawy dołączana jest deklaracja zgodności oraz atest wyrobu, na życzenie także świadectwo odbioru zgodne z uzgodnioną normą.'),

('Czy posiadają Państwo świadectwa badań wyrobów wydane przez laboratorium zewnętrzne?',
 'Tak. Badania typu prowadzone są w laboratoriach akredytowanych, sprawozdania z badań udostępniane są na życzenie klienta.'),

-- === Lancuch dostaw ===
('Jak Państwo oceniają i kwalifikują swoich dostawców?',
 'Dostawcy podlegają wstępnej kwalifikacji obejmującej ocenę finansową, jakościową i zgodnościową, a następnie corocznej ocenie okresowej opartej na terminowości, jakości dostaw i liczbie reklamacji.'),

('Czy posiadają Państwo kodeks postępowania dla dostawców?',
 'Tak. Kodeks postępowania dla dostawców obejmuje prawa pracownicze, zakaz pracy dzieci i pracy przymusowej, standardy środowiskowe oraz zasady antykorupcyjne. Akceptacja kodeksu jest warunkiem współpracy.'),

('Czy w Państwa łańcuchu dostaw występują minerały konfliktowe?',
 'Prowadzona jest analiza pochodzenia surowców objętych regulacjami dotyczącymi minerałów konfliktowych. Dostawcy składają oświadczenia o pochodzeniu, weryfikowane raz w roku.'),

('Jaki jest Państwa plan zapewnienia ciągłości dostaw?',
 'Wdrożony plan ciągłości działania obejmuje alternatywne źródła surowców krytycznych, zapasy bezpieczeństwa dla pozycji o długim czasie realizacji oraz procedurę eskalacji przy zakłóceniach.'),

-- === Handlowe ===
('Jakie są Państwa standardowe terminy płatności?',
 'Standardowy termin płatności to 30 dni od daty wystawienia faktury. Inne terminy podlegają indywidualnym uzgodnieniom i ocenie kredytowej.'),

('Jaki jest typowy czas realizacji zamówienia?',
 'Dla pozycji katalogowych 2-3 tygodnie od potwierdzenia zamówienia. Dla wyrobów niestandardowych termin ustalany jest indywidualnie i potwierdzany w ofercie.'),

('Czy dysponują Państwo wystarczającą zdolnością produkcyjną dla naszego wolumenu?',
 'Bieżące wykorzystanie zdolności produkcyjnych pozostawia rezerwę pozwalającą na obsługę dodatkowych wolumenów. Ocena wykonalności dla konkretnego zapotrzebowania wydawana jest w ciągu 5 dni roboczych.'),

('W jakich walutach prowadzą Państwo rozliczenia?',
 'Rozliczenia prowadzone są w PLN, EUR i USD. Waluta kontraktu ustalana jest na etapie negocjacji handlowych.'),

('Czy udzielają Państwo gwarancji na wyroby?',
 'Tak. Standardowy okres gwarancji to 24 miesiące od daty dostawy, z zastrzeżeniem przestrzegania warunków magazynowania i montażu określonych w dokumentacji technicznej.'),

-- === Reklamacje ===
('Jak wygląda Państwa procedura rozpatrywania reklamacji?',
 'Reklamacja rejestrowana jest w systemie w dniu zgłoszenia. Potwierdzenie przyjęcia w ciągu 2 dni roboczych, stanowisko merytoryczne w ciągu 14 dni, w sprawach wymagających badań laboratoryjnych do 30 dni.'),

('Jaki jest Państwa wskaźnik reklamacji w stosunku do liczby dostaw?',
 'W ostatnim pełnym roku obrachunkowym udział dostaw reklamowanych wyniósł 0,42 procent, z czego uznanych 0,19 procent.'),

('Czy stosują Państwo analizę przyczyn źródłowych dla reklamacji?',
 'Tak. Dla reklamacji uznanych prowadzona jest analiza przyczyn źródłowych, a działania korygujące są weryfikowane pod kątem skuteczności po 3 miesiącach.'),

-- === Audyty i wizyty ===
('Czy dopuszczają Państwo audyt drugiej strony przeprowadzony przez klienta?',
 'Tak. Audyty klienckie realizowane są po uprzednim uzgodnieniu terminu i zakresu, z zachowaniem zasad bezpieczeństwa obowiązujących na terenie zakładu.'),

('Ile czasu potrzebują Państwo na przygotowanie się do audytu klienta?',
 'Standardowo 15 dni roboczych od uzgodnienia zakresu. W przypadku audytów obejmujących badania wyrobu termin może się wydłużyć do 25 dni roboczych.');
