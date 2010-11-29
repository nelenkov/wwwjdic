CREATE TABLE "cardModels" (
	id INTEGER NOT NULL, 
	ordinal INTEGER NOT NULL, 
	"modelId" INTEGER NOT NULL, 
	name TEXT NOT NULL, 
	description TEXT NOT NULL, 
	active BOOLEAN NOT NULL, 
	qformat TEXT NOT NULL, 
	aformat TEXT NOT NULL, 
	lformat TEXT, 
	qedformat TEXT, 
	aedformat TEXT, 
	"questionInAnswer" BOOLEAN NOT NULL, 
	"questionFontFamily" TEXT, 
	"questionFontSize" INTEGER, 
	"questionFontColour" VARCHAR(7), 
	"questionAlign" INTEGER, 
	"answerFontFamily" TEXT, 
	"answerFontSize" INTEGER, 
	"answerFontColour" VARCHAR(7), 
	"answerAlign" INTEGER, 
	"lastFontFamily" TEXT, 
	"lastFontSize" INTEGER, 
	"lastFontColour" VARCHAR(7), 
	"editQuestionFontFamily" TEXT, 
	"editQuestionFontSize" INTEGER, 
	"editAnswerFontFamily" TEXT, 
	"editAnswerFontSize" INTEGER, 
	"allowEmptyAnswer" BOOLEAN NOT NULL, 
	"typeAnswer" TEXT NOT NULL, 
	PRIMARY KEY (id), 
	 FOREIGN KEY("modelId") REFERENCES models (id)
);
CREATE TABLE cardTags (
id integer not null,
cardId integer not null,
tagId integer not null,
src integer not null,
primary key(id));
CREATE TABLE cards (
	id INTEGER NOT NULL, 
	"factId" INTEGER NOT NULL, 
	"cardModelId" INTEGER NOT NULL, 
	created NUMERIC(10, 2) NOT NULL, 
	modified NUMERIC(10, 2) NOT NULL, 
	tags TEXT NOT NULL, 
	ordinal INTEGER NOT NULL, 
	question TEXT NOT NULL, 
	answer TEXT NOT NULL, 
	priority INTEGER NOT NULL, 
	interval NUMERIC(10, 2) NOT NULL, 
	"lastInterval" NUMERIC(10, 2) NOT NULL, 
	due NUMERIC(10, 2) NOT NULL, 
	"lastDue" NUMERIC(10, 2) NOT NULL, 
	factor NUMERIC(10, 2) NOT NULL, 
	"lastFactor" NUMERIC(10, 2) NOT NULL, 
	"firstAnswered" NUMERIC(10, 2) NOT NULL, 
	reps INTEGER NOT NULL, 
	successive INTEGER NOT NULL, 
	"averageTime" NUMERIC(10, 2) NOT NULL, 
	"reviewTime" NUMERIC(10, 2) NOT NULL, 
	"youngEase0" INTEGER NOT NULL, 
	"youngEase1" INTEGER NOT NULL, 
	"youngEase2" INTEGER NOT NULL, 
	"youngEase3" INTEGER NOT NULL, 
	"youngEase4" INTEGER NOT NULL, 
	"matureEase0" INTEGER NOT NULL, 
	"matureEase1" INTEGER NOT NULL, 
	"matureEase2" INTEGER NOT NULL, 
	"matureEase3" INTEGER NOT NULL, 
	"matureEase4" INTEGER NOT NULL, 
	"yesCount" INTEGER NOT NULL, 
	"noCount" INTEGER NOT NULL, 
	"spaceUntil" NUMERIC(10, 2) NOT NULL, 
	"relativeDelay" NUMERIC(10, 2) NOT NULL, 
	"isDue" BOOLEAN NOT NULL, 
	type INTEGER NOT NULL, 
	"combinedDue" INTEGER NOT NULL, 
	PRIMARY KEY (id), 
	 FOREIGN KEY("factId") REFERENCES facts (id), 
	 FOREIGN KEY("cardModelId") REFERENCES "cardModels" (id)
);
CREATE TABLE "cardsDeleted" (
	"cardId" INTEGER NOT NULL, 
	"deletedTime" NUMERIC(10, 2) NOT NULL, 
	 FOREIGN KEY("cardId") REFERENCES cards (id)
);
CREATE TABLE "deckVars" (
	"key" TEXT NOT NULL, 
	value TEXT, 
	PRIMARY KEY ("key")
);
CREATE TABLE decks (
	id INTEGER NOT NULL, 
	created NUMERIC(10, 2) NOT NULL, 
	modified NUMERIC(10, 2) NOT NULL, 
	description TEXT NOT NULL, 
	version INTEGER NOT NULL, 
	"currentModelId" INTEGER, 
	"syncName" TEXT, 
	"lastSync" NUMERIC(10, 2) NOT NULL, 
	"hardIntervalMin" NUMERIC(10, 2) NOT NULL, 
	"hardIntervalMax" NUMERIC(10, 2) NOT NULL, 
	"midIntervalMin" NUMERIC(10, 2) NOT NULL, 
	"midIntervalMax" NUMERIC(10, 2) NOT NULL, 
	"easyIntervalMin" NUMERIC(10, 2) NOT NULL, 
	"easyIntervalMax" NUMERIC(10, 2) NOT NULL, 
	delay0 INTEGER NOT NULL, 
	delay1 INTEGER NOT NULL, 
	delay2 NUMERIC(10, 2) NOT NULL, 
	"collapseTime" INTEGER NOT NULL, 
	"highPriority" TEXT NOT NULL, 
	"medPriority" TEXT NOT NULL, 
	"lowPriority" TEXT NOT NULL, 
	suspended TEXT NOT NULL, 
	"newCardOrder" INTEGER NOT NULL, 
	"newCardSpacing" INTEGER NOT NULL, 
	"failedCardMax" INTEGER NOT NULL, 
	"newCardsPerDay" INTEGER NOT NULL, 
	"sessionRepLimit" INTEGER NOT NULL, 
	"sessionTimeLimit" INTEGER NOT NULL, 
	"utcOffset" NUMERIC(10, 2) NOT NULL, 
	"cardCount" INTEGER NOT NULL, 
	"factCount" INTEGER NOT NULL, 
	"failedNowCount" INTEGER NOT NULL, 
	"failedSoonCount" INTEGER NOT NULL, 
	"revCount" INTEGER NOT NULL, 
	"newCount" INTEGER NOT NULL, 
	"revCardOrder" INTEGER NOT NULL, 
	PRIMARY KEY (id), 
	 FOREIGN KEY("currentModelId") REFERENCES models (id)
);
CREATE TABLE facts (
	id INTEGER NOT NULL, 
	"modelId" INTEGER NOT NULL, 
	created NUMERIC(10, 2) NOT NULL, 
	modified NUMERIC(10, 2) NOT NULL, 
	tags TEXT NOT NULL, 
	"spaceUntil" NUMERIC(10, 2) NOT NULL, 
	"lastCardId" INTEGER, 
	PRIMARY KEY (id), 
	 CONSTRAINT "lastCardIdfk" FOREIGN KEY("lastCardId") REFERENCES cards (id), 
	 FOREIGN KEY("modelId") REFERENCES models (id)
);
CREATE TABLE "factsDeleted" (
	"factId" INTEGER NOT NULL, 
	"deletedTime" NUMERIC(10, 2) NOT NULL, 
	 FOREIGN KEY("factId") REFERENCES facts (id)
);
CREATE TABLE "fieldModels" (
	id INTEGER NOT NULL, 
	ordinal INTEGER NOT NULL, 
	"modelId" INTEGER NOT NULL, 
	name TEXT NOT NULL, 
	description TEXT NOT NULL, 
	features TEXT NOT NULL, 
	required BOOLEAN NOT NULL, 
	"unique" BOOLEAN NOT NULL, 
	numeric BOOLEAN NOT NULL, 
	"quizFontFamily" TEXT, 
	"quizFontSize" INTEGER, 
	"quizFontColour" VARCHAR(7), 
	"editFontFamily" TEXT, 
	"editFontSize" INTEGER, 
	PRIMARY KEY (id), 
	 FOREIGN KEY("modelId") REFERENCES models (id)
);
CREATE TABLE fields (
	id INTEGER NOT NULL, 
	"factId" INTEGER NOT NULL, 
	"fieldModelId" INTEGER NOT NULL, 
	ordinal INTEGER NOT NULL, 
	value TEXT NOT NULL, 
	PRIMARY KEY (id), 
	 FOREIGN KEY("factId") REFERENCES facts (id), 
	 FOREIGN KEY("fieldModelId") REFERENCES "fieldModels" (id)
);
CREATE TABLE media (
	id INTEGER NOT NULL, 
	filename TEXT NOT NULL, 
	size INTEGER NOT NULL, 
	created NUMERIC(10, 2) NOT NULL, 
	"originalPath" TEXT NOT NULL, 
	description TEXT NOT NULL, 
	PRIMARY KEY (id)
);
CREATE TABLE "mediaDeleted" (
	"mediaId" INTEGER NOT NULL, 
	"deletedTime" NUMERIC(10, 2) NOT NULL, 
	 FOREIGN KEY("mediaId") REFERENCES cards (id)
);
CREATE TABLE models (
	id INTEGER NOT NULL, 
	"deckId" INTEGER, 
	created NUMERIC(10, 2) NOT NULL, 
	modified NUMERIC(10, 2) NOT NULL, 
	tags TEXT NOT NULL, 
	name TEXT NOT NULL, 
	description TEXT NOT NULL, 
	features TEXT NOT NULL, 
	spacing NUMERIC(10, 2) NOT NULL, 
	"initialSpacing" NUMERIC(10, 2) NOT NULL, 
	source INTEGER NOT NULL, 
	PRIMARY KEY (id), 
	 CONSTRAINT "deckIdfk" FOREIGN KEY("deckId") REFERENCES decks (id)
);
CREATE TABLE "modelsDeleted" (
	"modelId" INTEGER NOT NULL, 
	"deletedTime" NUMERIC(10, 2) NOT NULL, 
	 FOREIGN KEY("modelId") REFERENCES models (id)
);
CREATE TABLE "reviewHistory" (
	"cardId" INTEGER NOT NULL, 
	time NUMERIC(10, 2) NOT NULL, 
	"lastInterval" NUMERIC(10, 2) NOT NULL, 
	"nextInterval" NUMERIC(10, 2) NOT NULL, 
	ease INTEGER NOT NULL, 
	delay NUMERIC(10, 2) NOT NULL, 
	"lastFactor" NUMERIC(10, 2) NOT NULL, 
	"nextFactor" NUMERIC(10, 2) NOT NULL, 
	reps NUMERIC(10, 2) NOT NULL, 
	"thinkingTime" NUMERIC(10, 2) NOT NULL, 
	"yesCount" NUMERIC(10, 2) NOT NULL, 
	"noCount" NUMERIC(10, 2) NOT NULL, 
	PRIMARY KEY ("cardId", time)
);
CREATE TABLE sources (
	id INTEGER NOT NULL, 
	name TEXT NOT NULL, 
	created NUMERIC(10, 2) NOT NULL, 
	"lastSync" NUMERIC(10, 2) NOT NULL, 
	"syncPeriod" INTEGER NOT NULL, 
	PRIMARY KEY (id)
);
CREATE TABLE stats (
	id INTEGER NOT NULL, 
	type INTEGER NOT NULL, 
	day DATE NOT NULL, 
	reps INTEGER NOT NULL, 
	"averageTime" NUMERIC(10, 2) NOT NULL, 
	"reviewTime" NUMERIC(10, 2) NOT NULL, 
	"distractedTime" NUMERIC(10, 2) NOT NULL, 
	"distractedReps" INTEGER NOT NULL, 
	"newEase0" INTEGER NOT NULL, 
	"newEase1" INTEGER NOT NULL, 
	"newEase2" INTEGER NOT NULL, 
	"newEase3" INTEGER NOT NULL, 
	"newEase4" INTEGER NOT NULL, 
	"youngEase0" INTEGER NOT NULL, 
	"youngEase1" INTEGER NOT NULL, 
	"youngEase2" INTEGER NOT NULL, 
	"youngEase3" INTEGER NOT NULL, 
	"youngEase4" INTEGER NOT NULL, 
	"matureEase0" INTEGER NOT NULL, 
	"matureEase1" INTEGER NOT NULL, 
	"matureEase2" INTEGER NOT NULL, 
	"matureEase3" INTEGER NOT NULL, 
	"matureEase4" INTEGER NOT NULL, 
	PRIMARY KEY (id)
);
CREATE TABLE tags (
id integer not null,
tag text not null collate nocase,
priority integer not null default 2,
primary key(id));
CREATE VIEW acqCardsNew as
select * from cards
where type = 2 and isDue = 1
order by priority desc, due desc;
CREATE VIEW acqCardsOld as
select * from cards
where type = 2 and isDue = 1
order by priority desc, due;
CREATE VIEW failedCards as
select * from cards
where type = 0 and isDue = 1
order by type, isDue, combinedDue;
CREATE VIEW revCardsDue as
select * from cards
where type = 1 and isDue = 1
order by priority desc, due;
CREATE VIEW revCardsNew as
select * from cards
where type = 1 and isDue = 1
order by priority desc, interval;
CREATE VIEW revCardsOld as
select * from cards
where type = 1 and isDue = 1
order by priority desc, interval desc;
CREATE VIEW revCardsRandom as
select * from cards
where type = 1 and isDue = 1
order by priority desc, factId, ordinal;
CREATE INDEX ix_cardTags_cardId on cardTags (cardId);
CREATE INDEX ix_cardTags_tagCard on cardTags (tagId, cardId);
CREATE INDEX ix_cardsDeleted_cardId on cardsDeleted (cardId);
CREATE INDEX ix_cards_dueAsc on cards (type, isDue, priority desc, due);
CREATE INDEX ix_cards_duePriority on cards
(type, isDue, combinedDue, priority);
CREATE INDEX ix_cards_factId on cards (factId, type);
CREATE INDEX ix_cards_factor on cards
(type, factor);
CREATE INDEX ix_cards_intervalDesc on cards (type, isDue, priority desc, interval desc);
CREATE INDEX ix_cards_priorityDue on cards
(type, isDue, priority, combinedDue);
CREATE INDEX ix_factsDeleted_factId on factsDeleted (factId);
CREATE INDEX ix_fields_factId on fields (factId);
CREATE INDEX ix_fields_fieldModelId on fields (fieldModelId);
CREATE INDEX ix_fields_value on fields (value);
CREATE INDEX ix_mediaDeleted_factId on mediaDeleted (mediaId);
CREATE UNIQUE INDEX ix_media_filename on media (filename);
CREATE INDEX ix_media_originalPath on media (originalPath);
CREATE INDEX ix_modelsDeleted_modelId on modelsDeleted (modelId);
CREATE INDEX ix_stats_typeDay on stats (type, day);
CREATE UNIQUE INDEX ix_tags_tag on tags (tag);
